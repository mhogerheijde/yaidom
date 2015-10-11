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

package eu.cdevreeze.yaidom.queryapi

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * API and implementation trait for functionally updatable elements. This trait extends trait [[eu.cdevreeze.yaidom.queryapi.IsNavigable]],
 * adding knowledge about child nodes in general, and about the correspondence between child path entries and child
 * indexes.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `children`, `withChildren` and `childNodeIndex`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for functionally updating elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends ClarkElemLike[E] with UpdatableElemApi[N, E] { self: E =>

  // TODO Rename to UpdatableClarkElemLike

  def children: immutable.IndexedSeq[N]

  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  def childNodeIndex(childPathEntry: Path.Entry): Int

  final def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): E = {
    withChildren(newChildSeqs.flatten)
  }

  final def withUpdatedChildren(index: Int, newChild: N): E =
    withChildren(children.updated(index, newChild))

  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
    withChildren(children.patch(from, newChildren, replace))

  final def plusChild(index: Int, child: N): E = {
    require(
      index <= self.children.size,
      s"Expected index $index to be at most the number of children: ${self.children.size}")

    if (index == children.size) plusChild(child)
    else withPatchedChildren(index, Vector(child, children(index)), 1)
  }

  final def plusChild(child: N): E = withChildren(children :+ child)

  final def plusChildOption(index: Int, childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(index, childOption.get)
  }

  final def plusChildOption(childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(childOption.get)
  }

  final def plusChildren(childSeq: immutable.IndexedSeq[N]): E = {
    withChildren(children ++ childSeq)
  }

  final def minusChild(index: Int): E = {
    require(
      index < self.children.size,
      s"Expected index $index to be less than the number of children: ${self.children.size}")

    withPatchedChildren(index, Vector(), 1)
  }

  final def updated(pathEntry: Path.Entry)(f: E => E): E = {
    val idx = self.childNodeIndex(pathEntry)
    require(idx >= 0, "Expected non-negative child node index")

    self.withUpdatedChildren(idx, f(children(idx).asInstanceOf[E]))
  }

  final def updatedAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E = {
    updateChildElems(pathEntries)(f)
  }

  final def updated(path: Path)(f: E => E): E = {
    if (path == Path.Root) f(self)
    else {
      // Recursive, but not tail-recursive

      updated(path.firstEntry) { e => e.updated(path.withoutFirstEntry)(f) }
    }
  }

  final def updated(path: Path, newElem: E): E = updated(path) { e => newElem }

  final def updatedAtPaths(paths: Set[Path])(f: (E, Path) => E): E = {
    updateElemsOrSelf(paths)(f)
  }

  final def updatedWithNodeSeqIfPathNonEmpty(path: Path)(f: E => immutable.IndexedSeq[N]): E = {
    if (path == Path.Root) self
    else {
      assert(path.parentPathOption.isDefined)
      val parentPath = path.parentPath
      val parentElem = findElemOrSelfByPath(parentPath).getOrElse(sys.error(s"Incorrect parent path $parentPath"))

      val lastEntry = path.lastEntry
      val childNodeIndex = parentElem.childNodeIndex(lastEntry)
      require(childNodeIndex >= 0, s"Incorrect path entry $lastEntry")

      val childElemOption = parentElem.findChildElemByPathEntry(lastEntry)
      assert(childElemOption.isDefined)
      val childElem = childElemOption.get

      updated(parentPath, parentElem.withPatchedChildren(childNodeIndex, f(childElem), 1))
    }
  }

  @deprecated(message = "Renamed to 'updatedWithNodeSeqIfPathNonEmpty'", since = "1.5.0")
  final def updatedWithNodeSeq(path: Path)(f: E => immutable.IndexedSeq[N]): E = updatedWithNodeSeqIfPathNonEmpty(path)(f)

  final def updatedWithNodeSeqIfPathNonEmpty(path: Path, newNodes: immutable.IndexedSeq[N]): E =
    updatedWithNodeSeqIfPathNonEmpty(path) { e => newNodes }

  @deprecated(message = "Renamed to 'updatedWithNodeSeqIfPathNonEmpty'", since = "1.5.0")
  final def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[N]): E = updatedWithNodeSeqIfPathNonEmpty(path, newNodes)

  final def updatedWithNodeSeqAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E = {
    updateChildElemsWithNodeSeq(pathEntries)(f)
  }

  final def updatedWithNodeSeqAtNonEmptyPaths(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E = {
    updateElemsWithNodeSeq(paths)(f)
  }

  @deprecated(message = "Renamed to 'updatedWithNodeSeqAtNonEmptyPaths'", since = "1.5.0")
  final def updatedWithNodeSeqAtPaths(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E =
    updatedWithNodeSeqAtNonEmptyPaths(paths)(f)

  final def updateChildElems(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E =
    updateChildElems { case (che, pathEntry) => if (pathEntries.contains(pathEntry)) Some(f(che, pathEntry)) else None }

  final def updateChildElemsWithNodeSeq(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E =
    updateChildElemsWithNodeSeq { case (che, pathEntry) => if (pathEntries.contains(pathEntry)) Some(f(che, pathEntry)) else None }

  final def updateElemsOrSelf(paths: Set[Path])(f: (E, Path) => E): E =
    updateElemsOrSelf { case (e, path) => if (paths.contains(path)) Some(f(e, path)) else None }

  final def updateElems(paths: Set[Path])(f: (E, Path) => E): E =
    updateElems { case (e, path) => if (paths.contains(path)) Some(f(e, path)) else None }

  final def updateElemsOrSelfWithNodeSeq(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] =
    updateElemsOrSelfWithNodeSeq { case (e, path) => if (paths.contains(path)) Some(f(e, path)) else None }

  final def updateElemsWithNodeSeq(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E =
    updateElemsWithNodeSeq { case (e, path) => if (paths.contains(path)) Some(f(e, path)) else None }

  final def updateChildElems(f: (E, Path.Entry) => Option[E]): E =
    optionallyUpdateChildElems(f).getOrElse(self)

  final def updateChildElemsWithNodeSeq(f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): E =
    optionallyUpdateChildElemsWithNodeSeq(f).getOrElse(self)

  final def updateElemsOrSelf(f: (E, Path) => Option[E]): E =
    optionallyUpdateElemsOrSelf(f).getOrElse(self)

  final def updateElems(f: (E, Path) => Option[E]): E =
    optionallyUpdateElems(f).getOrElse(self)

  final def updateElemsOrSelfWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): immutable.IndexedSeq[N] =
    optionallyUpdateElemsOrSelfWithNodeSeq(f).getOrElse(Vector(self))

  final def updateElemsWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): E =
    optionallyUpdateElemsWithNodeSeq(f).getOrElse(self)

  final def optionallyUpdateChildElems(f: (E, Path.Entry) => Option[E]): Option[E] = {
    optionallyUpdateChildElemsWithNodeSeq { case (che, pe) => f(che, pe).map(e => Vector(e)) }
  }

  final def optionallyUpdateChildElemsWithNodeSeq(f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): Option[E] = {
    val nodeSeqsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[N]] =
      findAllChildElemsWithPathEntries.map({ case (che, pe) => (pe, f(che, pe)) }).
        collect({ case (pe, Some(nodes)) => (pe, nodes) }).toMap

    if (nodeSeqsByPathEntries.isEmpty) None
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        nodeSeqsByPathEntries.keySet.toSeq.map(entry => (entry -> childNodeIndex(entry))).sortBy(_._2)

      require(indexesByPathEntries.forall(_._2 >= 0), "Expected only non-negative child node indexes")

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[E]
          // Expensive assertion
          assert(findChildElemByPathEntry(pathEntry) == Some(che))
          val newNodesOption = nodeSeqsByPathEntries.get(pathEntry)
          assert(newNodesOption.isDefined)
          accChildNodes.patch(idx, newNodesOption.get, 1)
      }
      Some(self.withChildren(newChildren))
    }
  }

  final def optionallyUpdateElemsOrSelf(f: (E, Path) => Option[E]): Option[E] = {
    val descendantUpdateResult =
      optionallyUpdateChildElems {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          che optionallyUpdateElemsOrSelf {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    descendantUpdateResult.map(e => f(e, Path.Root).getOrElse(e)).orElse(f(self, Path.Root))
  }

  final def optionallyUpdateElems(f: (E, Path) => Option[E]): Option[E] = {
    optionallyUpdateChildElems {
      case (che, pathEntry) =>
        che optionallyUpdateElemsOrSelf {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def optionallyUpdateElemsOrSelfWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): Option[immutable.IndexedSeq[N]] = {
    val descendantUpdateResult =
      optionallyUpdateChildElemsWithNodeSeq {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          che optionallyUpdateElemsOrSelfWithNodeSeq {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    descendantUpdateResult.map(e => f(e, Path.Root).getOrElse(Vector(e))).orElse(f(self, Path.Root))
  }

  final def optionallyUpdateElemsWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): Option[E] = {
    optionallyUpdateChildElemsWithNodeSeq {
      case (che, pathEntry) =>
        che optionallyUpdateElemsOrSelfWithNodeSeq {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }
}
