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

import scala.collection.{ immutable, mutable }
import scala.annotation.tailrec

/**
 * Unique identification of a descendant (or self) `Elem` given a root `Elem`. It is used for transformations
 * from one node tree to another collection of nodes.
 *
 * An [[eu.cdevreeze.yaidom.ElemPath]] corresponds to one and only one canonical path of the element (modulo prefix names),
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * There is one catch, though. The `ElemPath` does not know the root element name, so that is not a part of the corresponding
 * canonical XPath expression. See the documentation of method `toCanonicalXPath`.
 *
 * The `ElemPath` contains an `IndexedSeq` of path entries for a specific child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 *
 * Strictly speaking, each element in a tree would be uniquely identified by path entries that only contained
 * a child index instead of an element name plus child index (of children with the given name). Yet that would
 * be far less easy to use. Hence `ElemPath.Entry` instances each contain an element name plus index.
 *
 * @author Chris de Vreeze
 */
final class ElemPath(val entries: immutable.IndexedSeq[ElemPath.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Returns true if this is the root `ElemPath`, so if it has no entries */
  def isRoot: Boolean = entries.isEmpty

  /** Prepends a given `Entry` to this `ElemPath` */
  def prepend(entry: ElemPath.Entry): ElemPath = ElemPath(entry +: self.entries)

  /** Returns the `ElemPath` with the first path entry (if any) removed, wrapped in an `Option`. */
  def withoutFirstEntryOption: Option[ElemPath] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(ElemPath(entries.tail))
  }

  /** Like `withoutFirstEntryOption`, but unwrapping the result (or throwing an exception otherwise) */
  def withoutFirstEntry: ElemPath = withoutFirstEntryOption.getOrElse(sys.error("The root path has no first entry to remove"))

  /** Appends a given `Entry` to this `ElemPath` */
  def append(entry: ElemPath.Entry): ElemPath = ElemPath(self.entries :+ entry)

  /**
   * Gets the parent path (if any, because the root path has no parent) wrapped in an `Option`.
   *
   * This method shows much of the reason why class `ElemPath` exists. If we know an element's `ElemPath`, and therefore its
   * parent `ElemPath` (using this method), then we can obtain the parent element by following the parent path from the
   * root of the tree.
   */
  def parentPathOption: Option[ElemPath] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(ElemPath(entries.dropRight(1)))
  }

  /** Like `parentPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: ElemPath = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /** Returns the ancestor-or-self paths, starting with this path, then the parent (if any), and ending with the root path */
  def ancestorOrSelfPaths: immutable.IndexedSeq[ElemPath] = {
    @tailrec
    def accumulate(path: ElemPath, acc: mutable.ArrayBuffer[ElemPath]): mutable.ArrayBuffer[ElemPath] = {
      acc :+ path
      if (path.isRoot) acc else accumulate(path.parentPath, acc)
    }

    accumulate(self, mutable.ArrayBuffer[ElemPath]()).toIndexedSeq
  }

  /** Returns the ancestor paths, starting with the parent path (if any), and ending with the root path */
  def ancestorPaths: immutable.IndexedSeq[ElemPath] = ancestorOrSelfPaths.drop(1)

  /** Returns the first entry, if any, wrapped in an `Option` */
  def firstEntryOption: Option[ElemPath.Entry] = entries.headOption

  /** Returns the first entry, if any, and throws an exception otherwise */
  def firstEntry: ElemPath.Entry = firstEntryOption.getOrElse(sys.error("There are no entries"))

  /** Returns the last entry, if any, wrapped in an `Option` */
  def lastEntryOption: Option[ElemPath.Entry] = entries.takeRight(1).headOption

  /** Returns the last entry, if any, and throws an exception otherwise */
  def lastEntry: ElemPath.Entry = lastEntryOption.getOrElse(sys.error("There are no entries"))

  /** Convenience method returning true if the first entry (if any) has the given element name */
  def startsWithName(ename: EName): Boolean = firstEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if the last entry (if any) has the given element name */
  def endsWithName(ename: EName): Boolean = lastEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if at least one entry has the given element name */
  def containsName(ename: EName): Boolean = entries exists { entry => entry.elementName == ename }

  override def equals(obj: Any): Boolean = obj match {
    case other: ElemPath =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  override def toString: String = entries.toString

  /**
   * Given an invertible `Scope`, returns the corresponding canonical XPath, but modified for the root element (which is unknown in the `ElemPath`).
   * The modification is that the root element is written as a slash followed by an asterisk.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
   */
  def toCanonicalXPath(scope: Scope): String = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    val entryXPaths = entries map { entry => entry.toCanonicalXPath(scope) }
    "/" + "*" + entryXPaths.mkString
  }
}

object ElemPath {

  val Root: ElemPath = ElemPath(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[ElemPath.Entry]): ElemPath = new ElemPath(entries)

  /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
  def apply(s: String)(scope: Scope): ElemPath = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    fromCanonicalXPath(s)(scope)
  }

  /** Easy to use factory method for `ElemPath` instances */
  def from(entries: ElemPath.Entry*): ElemPath = new ElemPath(Vector(entries: _*))

  /** Creates an `ElemPath` from a sequence of entry XPath strings. The passed scope must be invertible. */
  def fromXPaths(paths: immutable.Seq[String])(scope: Scope): ElemPath = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    val entries = paths map { path => ElemPath.Entry(path)(scope) }
    apply(entries.toIndexedSeq)
  }

  /** Parses a String, which must be in the `toCanonicalXPath` format, into an `ElemPath`. The passed scope must be invertible. */
  def fromCanonicalXPath(s: String)(scope: Scope): ElemPath = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    // We use the fact that "/", "*", "[" and "]" are never part of qualified names!

    require(s.startsWith("/"), "The canonical XPath must start with a slash")
    require(s.drop(1).startsWith("*"), "The canonical XPath must have an asterisk after the starting slash")
    val remainder = s.drop(2)
    require(remainder.headOption.forall(_ == '/'), "The canonical XPath's third character, if any, must be a slash")

    def getEntryStrings(str: String): List[String] = str match {
      case "" => Nil
      case _ =>
        val idx = str indexWhere { c => c == ']' }
        require(idx > 0, "The canonical XPath must have positions for each 'entry', such as [1]")
        val curr = str.take(idx + 1)
        val rest = str.drop(idx + 1)
        require(rest.size == 0 || rest.startsWith("/"), "In the canonical XPath, after a position, either nothing or a slash follows")
        curr :: getEntryStrings(rest)
    }

    val entryStrings = getEntryStrings(remainder).toIndexedSeq
    val entries = entryStrings map { entryString => ElemPath.Entry.fromCanonicalXPath(entryString)(scope) }
    ElemPath(entries)
  }

  /** An entry in an `ElemPath`, as an expanded element name plus zero-based index of the elem as child element (with that name) of the parent. */
  final case class Entry(elementName: EName, index: Int) extends Immutable {

    require(elementName ne null)
    require(index >= 0)

    /** Position (1-based) of the element as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /** Given an invertible `Scope`, returns the corresponding canonical XPath */
    def toCanonicalXPath(scope: Scope): String = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      val prefixOption: Option[String] = {
        if (elementName.namespaceUriOption.isEmpty) None else {
          val nsUri: String = elementName.namespaceUriOption.get
          require((scope.map - "").values.toSet.contains(nsUri), "Expected at least one prefix for namespace URI '%s'".format(nsUri))

          val result = (scope.map - "").toList collectFirst {
            case pair if pair._2 == nsUri =>
              val prefix: String = pair._1
              val ns: String = pair._2
              prefix
          }
          require(result.isDefined)
          result
        }
      }

      "%s%s[%d]".format("/", elementName.toQName(prefixOption).toString, position)
    }

    def localName: String = elementName.localPart
  }

  object Entry {

    /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
    def apply(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      fromCanonicalXPath(s)(scope)
    }

    /** Parses a `String`, which must be in the `toCanonicalXPath` format, into an `ElemPath.Entry`, given an invertible `Scope` */
    def fromCanonicalXPath(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      // We use the fact that "/", "[" and "]" are never part of qualified names!

      require(s.startsWith("/"), "The canonical XPath for the 'entry' must start with a slash")
      val remainder = s.drop(1)
      require(remainder.size > 3, "The canonical XPath for the 'entry' must contain at least 4 characters")
      val (qnameString, positionString) = remainder span { c => c != '[' }
      require(positionString.size >= 3, "The canonical XPath for the 'entry' must have a position of at least 3 characters, such as [1]")
      require(positionString.startsWith("["), "The canonical XPath for the 'entry' must have a position starting with '[', such as [1]")
      require(positionString.endsWith("]"), "The canonical XPath for the 'entry' must have a position ending with ']', such as [1]")

      val qname = QName.parse(qnameString)
      val elementName = scope.resolveQName(qname).getOrElse(sys.error("Could not resolve QName '%s'".format(qname)))
      val position = positionString.drop(1).dropRight(1).toInt

      Entry(elementName, position - 1)
    }
  }
}
