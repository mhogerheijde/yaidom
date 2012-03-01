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

/**
 * Supertrait for [[eu.cdevreeze.yaidom.Elem]] and other element-like classes, such as [[eu.cdevreeze.yaidom.xlink.Elem]].
 * Below, we refer to these element-like objects as elements.
 *
 * The only abstract methods are `resolvedName`, `resolvedAttributes` and `allChildElems`.
 * Based on these methods alone, this trait offers a rich API for querying elements and attributes.
 *
 * This trait only knows about elements, not about nodes in general. Hence this trait has no knowledge about child nodes in
 * general. Hence the name `ElemAwareElemLike`.
 *
 * This trait offers public element retrieval methods to obtain:
 * <ul>
 * <li>child elements</li>
 * <li>descendant elements</li>
 * <li>descendant or self elements</li>
 * <li>first found descendant elements obeying a predicate, meaning that
 * they have no ancestors obeying that predicate</li>
 * </ul>
 * In the method names, "elems" stands for descendant elements, and "first elems" stands for first found descendant
 * elements as explained above.
 *
 * There are also attribute retrieval methods, methods for indexing the element tree and finding subtrees,
 * and methods dealing with ElemPaths.
 *
 * These element retrieval methods each have up to 3 variants (returning collections of elements):
 * <ol>
 * <li>A no argument variant, if applicable (typically with prefix "all" in the method name)</li>
 * <li>A variant taking a single `E => Boolean` predicate argument (with suffix "where" in the method name)</li>
 * <li>An variant taking an expanded name argument</li>
 * </ol>
 * The latter variant is implemented in terms of the variant that takes a single predicate argument.
 * Some methods also have variants that return a single element or an element `Option`, or that "collect" data by applying
 * a partial function.
 *
 * These element finder methods process and return elements in the following (depth-first) order:
 * <ol>
 * <li>Parents are processed before their children</li>
 * <li>Children are processed before the next sibling</li>
 * <li>The first child element is processed before the next child element, and so on</li>
 * </ol>
 * assuming that the no-arg `allChildElems` method returns the child elements in the correct order.
 * Hence, the methods taking a predicate invoke that predicate on the elements in a predictable order.
 * Per visited element, the predicate is invoked only once. These properties are especially important
 * if the predicate has side-effects, which typically should not be the case.
 *
 * @tparam E the (self) type of the element, so the type of the `ElemAwareElemLike[E]` itself
 *
 * @author Chris de Vreeze
 */
trait ElemAwareElemLike[E <: ElemAwareElemLike[E]] { self: E =>

  /** Resolved name of the element, as `ExpandedName` */
  def resolvedName: ExpandedName

  /** The attributes as a `Map` from `ExpandedName`s (instead of `QName`s) to values */
  def resolvedAttributes: Map[ExpandedName, String]

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ElemAwareElemLike` methods will be. */
  def allChildElems: immutable.IndexedSeq[E]

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  final def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements obeying the given predicate */
  final def childElemsWhere(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems filter p

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.IndexedSeq[E] = childElemsWhere { e => e.resolvedName == expandedName }

  /** Returns `allChildElems collect pf` */
  final def collectFromChildElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] = allChildElems collect pf

  /** Returns the single child element with the given expanded name, if any, wrapped in an `Option` */
  final def childElemOption(expandedName: ExpandedName): Option[E] = {
    val result = childElems(expandedName)
    require(result.size <= 1, "Expected at most 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.headOption
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def childElem(expandedName: ExpandedName): E = {
    val result = childElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns this element followed by all descendant elements */
  final def allElemsOrSelf: immutable.IndexedSeq[E] = allElemsOrSelfSeq

  /**
   * Returns those elements among this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to `allElemsOrSelf filter p`.
   */
  final def elemsOrSelfWhere(p: E => Boolean): immutable.IndexedSeq[E] = elemsOrSelfSeqWhere(p)

  /** Returns those elements among this element and its descendant elements that have the given expanded name */
  final def elemsOrSelf(expandedName: ExpandedName): immutable.IndexedSeq[E] = elemsOrSelfWhere { e => e.resolvedName == expandedName }

  /** Returns (the equivalent of) `allElemsOrSelf collect pf` */
  final def collectFromElemsOrSelf[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    elemsOrSelfWhere { e => pf.isDefinedAt(e) } collect pf

  /** Returns all descendant elements (not including this element). Same as `allElemsOrSelf.drop(1)` */
  final def allElems: immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch.allElemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, `allElems filter p` */
  final def elemsWhere(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch elemsOrSelfWhere p }

  /** Returns the descendant elements with the given expanded name */
  final def elems(expandedName: ExpandedName): immutable.IndexedSeq[E] = elemsWhere { e => e.resolvedName == expandedName }

  /** Returns (the equivalent of) `allElems collect pf` */
  final def collectFromElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    elemsWhere { e => pf.isDefinedAt(e) } collect pf

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def firstElemsWhere(p: E => Boolean): immutable.IndexedSeq[E] =
    allChildElems flatMap { ch => ch firstElemsOrSelfSeqWhere p }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def firstElems(expandedName: ExpandedName): immutable.IndexedSeq[E] = firstElemsWhere { e => e.resolvedName == expandedName }

  /** Returns the first found descendant element obeying the given predicate, if any, wrapped in an `Option` */
  final def firstElemOptionWhere(p: E => Boolean): Option[E] = {
    self.allChildElems.view flatMap { ch => ch firstElemOrSelfOptionWhere p } headOption
  }

  /** Returns the first found descendant element with the given expanded name, if any, wrapped in an `Option` */
  final def firstElemOption(expandedName: ExpandedName): Option[E] = firstElemOptionWhere { e => e.resolvedName == expandedName }

  /**
   * Finds the parent element, if any, searching in the tree with the given root element.
   * The implementation uses the `equals` method on the self type, and uses no index. Typically rather expensive.
   */
  final def findParentInTree(root: E): Option[E] = {
    root firstElemOrSelfOptionWhere { e => e.allChildElems exists { ch => ch == self } }
  }

  /** Computes an index on the given function taking an element, for example a function returning some unique element "identifier" */
  final def getIndex[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = allElemsOrSelf groupBy f

  /** Computes an index to parent elements, on the given function applied to the child elements */
  final def getIndexToParent[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = {
    val parentChildPairs = allElemsOrSelf flatMap { e => e.allChildElems map { ch => (e -> ch) } }
    parentChildPairs groupBy { pair => f(pair._2) } mapValues { pairs => pairs map { _._1 } } mapValues { _.distinct }
  }

  /**
   * Returns the equivalent of `findWithElemPath(ElemPath(immutable.IndexedSeq(entry)))`, but it should be more efficient.
   */
  final def findWithElemPathEntry(entry: ElemPath.Entry): Option[E] = {
    val relevantChildElms = self.childElems(entry.elementName)

    if (entry.index >= relevantChildElms.size) None else Some(relevantChildElms(entry.index))
  }

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   */
  final def findWithElemPath(path: ElemPath): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    def findWithElemPath(currentRoot: E, entryIndex: Int): Option[E] = {
      require(entryIndex >= 0 && entryIndex <= path.entries.size)

      if (entryIndex == path.entries.size) Some(currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findWithElemPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot => findWithElemPath(newRoot, entryIndex + 1) }
      }
    }

    findWithElemPath(self, 0)
  }

  /** Returns the `ElemPath` entries of all child elements, in the correct order */
  final def allChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry] = {
    // This implementation is O(n), where n is the number of children, and uses mutable collections for speed

    val elementNameCounts = mutable.Map[ExpandedName, Int]()
    val acc = mutable.ArrayBuffer[ElemPath.Entry]()

    for (elm <- self.allChildElems) {
      val countForName = elementNameCounts.getOrElse(elm.resolvedName, 0)
      val entry = ElemPath.Entry(elm.resolvedName, countForName)
      elementNameCounts.update(elm.resolvedName, countForName + 1)
      acc += entry
    }

    acc.toIndexedSeq
  }

  /**
   * Returns the `ElemPath` `Entry` of this element with respect to the given parent,
   * throwing an exception if this element is not a child of that parent.
   *
   * The implementation uses the equals method on the self type.
   */
  final def ownElemPathEntry(parent: E): ElemPath.Entry = {
    val idx = parent.childElems(self.resolvedName) indexWhere { e => e == self }
    require(idx >= 0, "Expected %s to have parent %s".format(self.toString, parent.toString))
    ElemPath.Entry(self.resolvedName, idx)
  }

  /** Returns an `IndexedSeq` of this element followed by all descendant elements */
  private final def allElemsOrSelfSeq: immutable.IndexedSeq[E] = {
    // Not tail-recursive, but the depth should typically be limited
    immutable.IndexedSeq(self) ++ {
      self.allChildElems flatMap { ch => ch.allElemsOrSelfSeq }
    }
  }

  /**
   * Returns an `IndexedSeq` of those of this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to `allElemsOrSelfSeq filter p`.
   */
  private final def elemsOrSelfSeqWhere(p: E => Boolean): immutable.IndexedSeq[E] = {
    // Not tail-recursive, but the depth should typically be limited
    val includesSelf = p(self)
    val resultWithoutSelf = self.allChildElems flatMap { ch => ch elemsOrSelfSeqWhere p }
    if (includesSelf) self +: resultWithoutSelf else resultWithoutSelf
  }

  /**
   * Returns an `IndexedSeq` of those of this element and its descendant elements that obey the given predicate,
   * such that no ancestor obeys the predicate.
   */
  private final def firstElemsOrSelfSeqWhere(p: E => Boolean): immutable.IndexedSeq[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) immutable.IndexedSeq(self) else self.allChildElems flatMap { ch => ch firstElemsOrSelfSeqWhere p }
  }

  /** Returns the first found descendant element or self obeying the given predicate, if any, wrapped in an `Option` */
  private final def firstElemOrSelfOptionWhere(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) Some(self) else self.allChildElems.view flatMap { ch => ch firstElemOrSelfOptionWhere p } headOption
  }
}
