/*
 * Copyright 2011 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom

import java.{ util => jutil }
import scala.annotation.tailrec
import scala.collection.immutable

/**
 * Immutable XML node. The API is inspired by Anti-XML, but it is less ambitious,
 * and differs in some key respects. Like Anti-XML:
 * <ul>
 * <li>Nodes in this API are truly immutable and thread-safe, backed by immutable
 * Scala collections.</li>
 * <li>Nodes have no reference to their parent/ancestor nodes. These nodes can be re-used in several
 * XML trees.</li>
 * <li>Documents are absent in both APIs, so "owning" documents are not modeled.
 * Comments are absent as well.</li>
 * </ul>
 * Unlike Anti-XML:
 * <ul>
 * <li>This is just a DOM-like API, around immutable Nodes and immutable Scala Collections of Nodes,
 * without any XPath support. Despite the absence of selectors like those in Anti-XML, this DOM API
 * is still quite expressive, be it with somewhat verbose method names. This API is also simpler
 * in that CanBuildFrom "axioms" are absent, and implicit conversions are absent or at least rare.
 * It is more verbose in that many ("DOM-like") convenience methods are offered.</li>
 * <li>This API distinguishes between QNames and ExpandedNames, making both first-class citizens in the API.
 * Moreover, Scopes are first-class citizens as well. By explicitly modeling QNames, ExpandedNames and Scopes,
 * the user of the API is somewhat shielded from some XML quirks.</li>
 * <li>This API is less ambitious. Like said above, XPath support is absent. So is support for "updates" through
 * zippers. So is true equality based on the exact tree. It is currently also less mature, and less well tested.</li>
 * </ul>
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable {

  /** The unique ID of the immutable Node (and its subtree). "Updates" result in new UUIDs. */
  val uuid: jutil.UUID
}

/**
 * Element node. An Elem consists of a QName of the element, the attributes mapping attribute QNames to String values,
 * a Scope mapping prefixes to namespace URIs, and an immutable collection of child Nodes. The element QName and attribute
 * QNames must be in scope, according to the passed Scope. The Scope is absolute, typically containing a lot more than
 * the (implicit) Scope.Declarations of this element.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 *
 * The API is geared towards data-oriented XML that uses namespaces, and that is described in schemas (so that the user of this
 * API knows the structure of the XML being processed). The methods that return an Option say so in their name.
 *
 * The constructor is private. See the apply factory method on the companion object, and its documentation.
 */
final class Elem private (
  val qname: QName,
  val attributes: Map[QName, String],
  val scope: Scope,
  val children: immutable.IndexedSeq[Node]) extends Node with ElemLike[Elem] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  /** The unique UUID of this Elem. Note that UUID creation turns out to suffer from poor concurrency due to locking */
  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  /** The attribute Scope, which is the same Scope but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = scope.copy(defaultNamespace = None)

  /** The Elem name as ExpandedName, obtained by resolving the element QName against the Scope */
  override val resolvedName: ExpandedName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an ExpandedName in scope [%s]".format(qname, scope)))

  /** The attributes as a Map from ExpandedNames (instead of QNames) to values, obtained by resolving attribute QNames against the attribute scope */
  val resolvedAttributes: Map[ExpandedName, String] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns the value of the attribute with the given expanded name, if any, and None otherwise */
  def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements */
  override def childElems: immutable.Seq[Elem] = children collect { case e: Elem => e }

  /** Returns the text children */
  def textChildren: immutable.Seq[Text] = children collect { case t: Text => t }

  /** Returns the first text child, if any, and None otherwise */
  def firstTextChildOption: Option[Text] = textChildren.headOption

  /** Returns the first text child's value, if any, and None otherwise */
  def firstTextValueOption: Option[String] = textChildren.headOption map { _.text }

  /** Returns the first text child, if any, and None otherwise */
  def firstTextChild: Text = firstTextChildOption.getOrElse(sys.error("Missing text child"))

  /** Returns the first text child's value, if any, and None otherwise */
  def firstTextValue: String = firstTextValueOption.getOrElse(sys.error("Missing text child"))

  /** Creates a copy, but with the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.Seq[Node]): Elem = new Elem(qname, attributes, scope, newChildren.toIndexedSeq)

  /** Copies this Elem, but on encountering a descendant (or self) matching the given partial function, invokes that function instead */
  def copy(root: Elem, f: PartialFunction[Elem.RootAndElem, Elem]): Elem = {
    // Recursive, but not tail-recursive. In practice, this should be no problem due to limited recursion depths.
    val rootAndElem = Elem.RootAndElem(root, self)

    if (f.isDefinedAt(rootAndElem)) f(rootAndElem) else {
      val newChildren = self.children map { (ch: Node) =>
        ch match {
          case ch: Elem => ch.copy(root, f)
          case n => n
        }
      }
      self.withChildren(newChildren)
    }
  }

  /** Computes an index on the UUID. Very inefficient. */
  def getIndexOnUuid: Map[jutil.UUID, Elem] = {
    val result = getIndex(e => e.uuid)
    require {
      result.values forall { elms => elms.size == 1 }
    }
    result mapValues { elms => elms(0) }
  }

  /**
   * Computes an index to parent elements, on the UUID of the child elements. Very inefficient.
   * After the index has been built, each element's parent can be obtained very quickly using this index.
   */
  def getIndexToParentOnUuid: Map[jutil.UUID, Elem] = {
    val result = getIndexToParent(e => e.uuid)
    require(!result.contains(self.uuid))
    require {
      result.values forall { elms => elms.size == 1 }
    }
    result mapValues { elms => elms(0) }
  }

  /** Equality based on the UUID. Fast but depends not only on the tree itself, but also the time of creation */
  override def equals(other: Any): Boolean = other match {
    case e: Elem => self.uuid == e.uuid
    case _ => false
  }

  /** Hash code, consistent with equals */
  override def hashCode: Int = uuid.hashCode

  /** Returns the XML string corresponding to this element, without the children (but ellipsis instead) */
  override def toString: String = withChildren(immutable.Seq[Node](Text(" ... "))).toXmlString(Scope.Empty)

  /** Returns the XML string corresponding to this element. Consider using an XML serializer (such as in JAXP) instead. */
  def toXmlString: String = toXmlString(Scope.Empty)

  /**
   * Returns the XML string corresponding to this element, taking the given parent Scope into account.
   * Consider using an XML serializer (such as in JAXP) instead.
   */
  def toXmlString(parentScope: Scope): String = toLines("", parentScope).mkString("%n".format())

  private def toLines(indent: String, parentScope: Scope): List[String] = {
    val declarations: Scope.Declarations = parentScope.relativize(self.scope)
    val declarationsString = declarations.toStringInXml
    val attrsString = attributes map { kv => """%s="%s"""".format(kv._1, kv._2) } mkString (" ")

    if (self.children.isEmpty) {
      val start = List(qname, declarationsString, attrsString) filterNot { _ == "" } mkString (" ")
      val line = "<%s />".format(start)
      List(line).map(ln => indent + ln)
    } else if (this.childElems.isEmpty) {
      val start = List(qname, declarationsString, attrsString) filterNot { _ == "" } mkString (" ")
      val content = children map { _.toString } mkString
      val line = "<%s>%s</%s>".format(start, content, qname)
      List(line).map(ln => indent + ln)
    } else {
      val start = List(qname, declarationsString, attrsString) filterNot { _ == "" } mkString (" ")
      val firstLine: String = "<%s>".format(start)
      val lastLine: String = "</%s>".format(qname)
      // Recursive (not tail-recursive) calls, ignoring non-element children
      val childElementLines: List[String] = self.childElems.toList flatMap { e => e.toLines("  ", self.scope) }
      (firstLine :: childElementLines ::: List(lastLine)) map { ln => indent + ln }
    }
  }
}

object Elem {

  final case class RootAndElem(root: Elem, elem: Elem) extends Immutable

  /**
   * Use this constructor with care, because it is easy to use incorrectly (w.r.t. passed Scopes).
   * To construct Elems, prefer using an ElemBuilder, via method <code>NodeBuilder.elem</code>.
   */
  def apply(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    scope: Scope = Scope.Empty,
    children: immutable.Seq[Node] = immutable.Seq()): Elem = new Elem(qname, attributes, scope, children.toIndexedSeq)
}

final case class Text(text: String) extends Node {
  require(text ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = text
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """<?%s %s?>""".format(target, data)
}

final case class CData(text: String) extends Node {
  require(text ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """<![CDATA[%s]]>""".format(text)
}

final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """&%s;""".format(entity)
}
