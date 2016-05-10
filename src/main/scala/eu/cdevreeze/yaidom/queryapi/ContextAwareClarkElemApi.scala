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

/**
 * Abstract API for "context-aware Clark elements".
 *
 * @tparam E The element type itself
 *
 * @author Chris de Vreeze
 */
trait ContextAwareClarkElemApi[E <: ContextAwareClarkElemApi[E]] extends ClarkElemApi[E] with ContextAwareApi { self: E =>
}

object ContextAwareClarkElemApi {

  /**
   * The `ContextAwareClarkElemApi` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] extends ClarkElemApi.FunctionApi[E] with ContextAwareApi.FunctionApi[E] {
  }
}
