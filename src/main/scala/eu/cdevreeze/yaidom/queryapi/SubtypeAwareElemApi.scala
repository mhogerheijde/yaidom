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
import scala.reflect.ClassTag

/**
 * Extension to ElemApi that makes querying for sub-types of the element type easy.
 *
 * For example, XML Schema can be modeled with an object hierarchy, starting with some XsdElem super-type which
 * mixes in trait SubtypeAwareElemApi, among other query traits. The object hierarchy could contain sub-classes
 * of XsdElem such as XsdRootElem, GlobalElementDeclaration, etc. Then the SubtypeAwareElemApi trait makes it
 * easy to query for all or some global element declarations, etc.
 *
 * There is no magic in these traits: it is just ElemApi and ElemLike underneath. It is only the syntactic
 * convenience that makes the difference.
 *
 * The query methods of this trait take a sub-type as first value parameter. It is intentional that this is a value
 * parameter, and not a second type parameter, since it is conceptually the most important parameter of these
 * query methods. (If it were a second type parameter instead, the article http://hacking-scala.org/post/73854628325/advanced-type-constraints-with-type-classes
 * would show how to make that solution robust, using some @NotNothing annotation.)
 *
 * The sub-type parameter could have been a `java.lang.Class` object, except that type erasure would make it less attractive
 * (when doing pattern matching against that type). Hence the use of a `ClassTag` parameter, which undoes type erasure
 * for non-generic types, if available implicitly. So `ClassTag` is used as a better `java.lang.Class`, yet without
 * polluting the public API with an implicit `ClassTag` parameter. (Instead, the ClassTag is made implicit inside the
 * method implementations.)
 *
 * @author Chris de Vreeze
 */
trait SubtypeAwareElemApi[A <: SubtypeAwareElemApi[A]] extends ElemApi[A] { self: A =>

  /**
   * Returns all child elements of the given sub-type, in the correct order.
   */
  def findAllChildElemsOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B]

  /**
   * Returns the child elements of the given sub-type obeying the given predicate.
   */
  def filterChildElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

  /**
   * Returns all descendant elements of the given sub-type (not including this element).
   */
  def findAllElemsOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B]

  /**
   * Returns the descendant elements of the given sub-type obeying the given predicate.
   */
  def filterElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

  /**
   * Returns all descendant-or-self elements of the given sub-type.
   */
  def findAllElemsOrSelfOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B]

  /**
   * Returns the descendant-or-self elements of the given sub-type obeying the given predicate.
   */
  def filterElemsOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

  /**
   * Returns the first found child element of the given sub-type obeying the given predicate, if any, wrapped in an `Option`.
   */
  def findChildElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B]

  /**
   * Returns the first found (topmost) descendant element of the given sub-type obeying the given predicate, if any, wrapped in an `Option`.
   */
  def findElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B]

  /**
   * Returns the first found (topmost) descendant-or-self element of the given sub-type obeying the given predicate, if any, wrapped in an `Option`.
   */
  def findElemOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B]

  /**
   * Returns the descendant elements of the given sub-type obeying the given predicate that have no ancestor of the given sub-type obeying the predicate.
   */
  def findTopmostElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

  /**
   * Returns the descendant-or-self elements of the given sub-type obeying the given predicate, such that no ancestor of the given sub-type obeys the predicate.
   */
  def findTopmostElemsOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

  /**
   * Returns the single child element of the given sub-type obeying the given predicate, and throws an exception otherwise.
   */
  def getChildElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): B
}

object SubtypeAwareElemApi {

  /**
   * The `SubtypeAwareElemApi` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[A] extends ElemApi.FunctionApi[A] {

    def findAllChildElemsOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B]

    def filterChildElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

    def findAllElemsOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B]

    def filterElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

    def findAllElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B]

    def filterElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

    def findChildElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B]

    def findElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B]

    def findElemOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B]

    def findTopmostElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

    def findTopmostElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B]

    def getChildElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): B
  }
}
