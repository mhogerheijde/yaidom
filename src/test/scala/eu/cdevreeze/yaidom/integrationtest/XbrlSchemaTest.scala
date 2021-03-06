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

package eu.cdevreeze.yaidom.integrationtest

import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax

/**
 * Test case using yaidom indexed elements for XBRL schema processing.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlSchemaTest extends Suite {
  import XbrlSchemaTest._

  def testQueryXbrlSchema(): Unit = {
    val parser = DocumentParserUsingSax.newInstance()
    val doc: Document = parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("gaap.xsd"))

    val xbrlSchemaDoc = indexed.Document(doc)
    val xbrlSchema: indexed.Elem = xbrlSchemaDoc.documentElement

    // Check concepts

    val elmDefs = xbrlSchema filterElemsOrSelf { e => e.resolvedName == EName(nsSchema, "element") }

    val tns = "http://xasb.org/gaap"

    assertResult(tns) {
      xbrlSchema.attributeOption(EName("targetNamespace")).getOrElse("")
    }

    assertResult(true) {
      // We query each element definition for the target namespace of the root (!) element
      elmDefs forall { e => e.rootElem.attributeOption(EName("targetNamespace")) == Some(tns) }
    }

    assertResult(Set(EName(tns, "AMinusMinusMember"), EName(tns, "APlusPlusPlusMember"))) {
      val conceptENames = elmDefs map { e =>
        // We query each element definition for the target namespace of the root (!) element
        val tnsOption = e.rootElem.attributeOption(EName("targetNamespace"))
        val localName = (e \@ EName("name")).getOrElse("")
        val result = EName(tnsOption, localName)
        result
      }
      val matchingConceptENames = conceptENames filter { ename => Set("AMinusMinusMember", "APlusPlusPlusMember").contains(ename.localPart) }
      matchingConceptENames.toSet
    }

    // Check equivalence of different ways to get the same concepts

    val paths = indexed.Elem(xbrlSchema.underlyingElem).findAllElemsOrSelf.map(_.path)

    assertResult(paths) {
      xbrlSchema.findAllElemsOrSelf map { _.path }
    }

    val elemsContainingPlus = xbrlSchema filterElems { e => e.attributeOption(EName("name")).getOrElse("").contains("Plus") }
    val pathsOfElemsContainingPlus =
      indexed.Elem(xbrlSchema.underlyingElem).filterElems(e => e.attributeOption(EName("name")).getOrElse("").contains("Plus")).map(_.path)

    assertResult(pathsOfElemsContainingPlus) {
      elemsContainingPlus map (_.path)
    }

    assertResult(true) {
      elemsContainingPlus forall { e => xbrlSchema.underlyingElem.findElemOrSelfByPath(e.path) == Some(e.underlyingElem) }
    }
  }
}

object XbrlSchemaTest {

  val nsSchema = "http://www.w3.org/2001/XMLSchema"
  val nsXbrli = "http://www.xbrl.org/2003/instance"
  val nsLink = "http://www.xbrl.org/2003/linkbase"
  val nsXLink = "http://www.w3.org/1999/xlink"
}
