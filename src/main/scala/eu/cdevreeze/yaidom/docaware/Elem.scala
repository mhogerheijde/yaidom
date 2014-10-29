/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.docaware

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.simple

/**
 * An element just like `indexed.Elem`, but storing the URI of the containing document as well. See [[eu.cdevreeze.yaidom.indexed.Elem]]
 * for more details. These details apply to [[eu.cdevreeze.yaidom.docaware.Elem]] too, except that additionally the document URI
 * is stored.
 *
 * @author Chris de Vreeze
 */
final class Elem private[docaware] (
  val docUri: URI,
  val rootElem: simple.Elem,
  childElems: immutable.IndexedSeq[Elem],
  val path: Path,
  val elem: simple.Elem) extends ScopedElemLike[Elem] with IsNavigable[Elem] with Immutable {

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(childElems.forall(_.docUri eq this.docUri), "Corrupt element!")
  }

  /**
   * Map from child node indexes to child elem indexes, for speeding up lookups of child elements
   */
  private val elemIndexesByNodeIndex: Map[Int, Int] = {
    (elem.children.zipWithIndex collect { case (e: simple.Elem, idx) => idx }).zipWithIndex.toMap
  }

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = childElems

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def findChildElemByPathEntry(entry: Path.Entry): Option[Elem] = {
    val nodeIdx = elem.childNodeIndex(entry)

    if (nodeIdx < 0) None
    else {
      val elemIdx = elemIndexesByNodeIndex(nodeIdx)
      val resultElem = childElems(elemIdx)

      assert(resultElem.resolvedName == entry.elementName)
      Some(resultElem)
    }
  }

  override def qname: QName = elem.qname

  override def attributes: immutable.IndexedSeq[(QName, String)] = elem.attributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem =>
      (other.docUri == this.docUri) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (docUri, rootElem, path).hashCode

  /**
   * Returns `this.elem.scope`
   */
  final override def scope: Scope = this.elem.scope

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }

  /**
   * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = elem.textChildren map { t => t.text }
    textStrings.mkString
  }

  /**
   * Returns the ENames of the ancestry-or-self, starting with the root element and ending with this element.
   *
   * That is, returns:
   * {{{
   * rootElem.resolvedName +: path.entries.map(_.elementName)
   * }}}
   */
  final def ancestryOrSelfENames: immutable.IndexedSeq[EName] = {
    rootElem.resolvedName +: path.entries.map(_.elementName)
  }

  /**
   * Returns the ENames of the ancestry, starting with the root element and ending with the parent of this element, if any.
   *
   * That is, returns:
   * {{{
   * ancestryOrSelfENames.dropRight(1)
   * }}}
   */
  final def ancestryENames: immutable.IndexedSeq[EName] = {
    ancestryOrSelfENames.dropRight(1)
  }
}

object Elem {

  /**
   * Calls `apply(docUri, rootElem, Path.Root)`
   */
  def apply(docUri: URI, rootElem: simple.Elem): Elem = {
    apply(docUri, rootElem, Path.Root)
  }

  /**
   * Expensive recursive factory method for "docaware elements".
   */
  def apply(docUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    val elem = rootElem.getElemOrSelfByPath(path)

    // Recursive calls
    val childElems = elem.findAllChildElemPathEntries.map(entry => apply(docUri, rootElem, path.append(entry)))

    new Elem(docUri, rootElem, childElems, path, elem)
  }
}
