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

package eu.cdevreeze.yaidom.indexed

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.IsNavigableApi
import eu.cdevreeze.yaidom.queryapi.NavigableClarkElemApi

/**
 * Abstract API for "indexed elements".
 *
 * @tparam E The element type itself
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
trait IndexedElemApi[E <: IndexedElemApi[E, U], U <: NavigableClarkElemApi[U]] extends ClarkElemApi[E] with IsNavigableApi[E] { self: E =>

  /**
   * The root element of the underlying element type
   */
  def rootElem: U

  /**
   * The path of this element, relative to the root element
   */
  def path: Path

  /**
   * The underlying element, of the underlying element type
   */
  def elem: U
}
