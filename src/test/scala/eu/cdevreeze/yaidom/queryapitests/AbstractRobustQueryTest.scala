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

package eu.cdevreeze.yaidom.queryapitests

import scala.Vector

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName

/**
 * ClarkElemLike-based query test case showing robust querying.
 * This example comes from http://www.cafeconleche.org/books/effectivexml/chapters/35.html.
 *
 * Yaidom also facilitates robust querying, but in a different and more explicit (and verbose)
 * way than XPath. Moreover, the yaidom queries work for multiple element implementations,
 * even element implementations as minimal as resolved elements.
 *
 * Interesting are the author's remarks about potential performance problems in XPath navigation
 * to ancestors of elements. Using Scala and yaidom, this is much less of an issue.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractRobustQueryTest extends Suite {

  type E <: ClarkElemLike.Aux[E]

  private val expectedNames = Vector(
    "John Doe",
    "Joe Smith",
    "Jane & Ben Reilly",
    "Mohammed Jones",
    "David Smith",
    "Jason Smith",
    "Xao Li",
    "Jonathan Li")

  @Test def testRobustQuerying(): Unit = {
    val contactNames =
      for {
        rootElem <- Option(contactsElem).toVector
        contact <- rootElem.filterChildElems(withLocalName("Contact"))
        name <- contact.filterChildElems(withLocalName("Name"))
      } yield name.normalizedText

    assertResult(expectedNames) {
      contactNames
    }
  }

  @Test def testEvenMoreRobustQuerying(): Unit = {
    val contactNames =
      for {
        name <- contactsElem.filterElems(withLocalName("Name"))
      } yield name.normalizedText

    assertResult(expectedNames) {
      contactNames
    }
  }

  protected val contactsElem: E

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem
}
