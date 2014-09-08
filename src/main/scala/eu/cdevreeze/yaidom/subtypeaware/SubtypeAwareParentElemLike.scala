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
package subtypeaware

import scala.collection.immutable
import scala.reflect.ClassTag

/**
 * Default implementation of SubtypeAwareParentElemApi.
 *
 * @author Chris de Vreeze
 */
trait SubtypeAwareParentElemLike[A <: SubtypeAwareParentElemLike[A]] extends ParentElemLike[A] with SubtypeAwareParentElemApi[A] { self: A =>

  final def findAllChildElemsTyped[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterChildElemsTyped(subType)((e: A) => true)
  }

  final def filterChildElemsTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterChildElems(pred) }
  }

  final def findAllElemsTyped[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsTyped(subType)((e: A) => true)
  }

  final def filterElemsTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElems(pred) }
  }

  final def findAllElemsOrSelfTyped[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsOrSelfTyped(subType)((e: A) => true)
  }

  final def filterElemsOrSelfTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElemsOrSelf(pred) }
  }

  final def findChildElemTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findChildElem(pred) }
  }

  final def findElemTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElem(pred) }
  }

  final def findElemOrSelfTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElemOrSelf(pred) }
  }

  final def findTopmostElemsTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElems(pred) }
  }

  final def findTopmostElemsOrSelfTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElemsOrSelf(pred) }
  }

  final def getChildElemTyped[B <: A](subType: ClassTag[B])(p: B => Boolean): B = {
    val result = findChildElemTyped(subType)(p)
    require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
    result.head
  }

  private final def filter[B <: A](
    subType: ClassTag[B])(p: B => Boolean)(f: ((A => Boolean) => immutable.IndexedSeq[A])): immutable.IndexedSeq[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct = subType

    val p2: (A => Boolean) = {
      case elem: B if p(elem) => true
      case _ => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }

  private final def find[B <: A](
    subType: ClassTag[B])(p: B => Boolean)(f: ((A => Boolean) => Option[A])): Option[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct = subType

    val p2: (A => Boolean) = {
      case elem: B if p(elem) => true
      case _ => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }
}
