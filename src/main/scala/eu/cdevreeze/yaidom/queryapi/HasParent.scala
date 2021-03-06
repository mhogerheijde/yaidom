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

import scala.collection.immutable

/**
 * Implementation trait for elements that can be asked for the ancestor elements, if any.
 *
 * This trait only knows about elements, not about documents as root element parents.
 *
 * Based on abstract method `parentOption` alone, this trait offers a rich API for querying the element ancestry of an element.
 *
 * @author Chris de Vreeze
 */
trait HasParent extends HasParentApi {

  type ThisElemApi <: HasParent

  /**
   * Returns the equivalent `parentOption.get`, throwing an exception if this is the root element
   */
  final def parent: ThisElem = parentOption.getOrElse(sys.error("There is no parent element"))

  /**
   * Returns all ancestor elements or self
   */
  final def ancestorsOrSelf: immutable.IndexedSeq[ThisElem] =
    thisElem +: (parentOption.toIndexedSeq flatMap ((e: ThisElem) => e.ancestorsOrSelf))

  /**
   * Returns `ancestorsOrSelf.drop(1)`
   */
  final def ancestors: immutable.IndexedSeq[ThisElem] = ancestorsOrSelf.drop(1)

  /**
   * Returns the first found ancestor-or-self element obeying the given predicate, if any, wrapped in an Option
   */
  // @tailrec
  final def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem] = {
    if (p(thisElem)) Some(thisElem) else {
      val optParent = parentOption
      if (optParent.isEmpty) None else optParent.get.findAncestorOrSelf(p)
    }
  }

  /**
   * Returns the first found ancestor element obeying the given predicate, if any, wrapped in an Option
   */
  final def findAncestor(p: ThisElem => Boolean): Option[ThisElem] = {
    parentOption flatMap { e => e.findAncestorOrSelf(p) }
  }
}

object HasParent {

  type Aux[A] = HasParent { type ThisElem = A }
}
