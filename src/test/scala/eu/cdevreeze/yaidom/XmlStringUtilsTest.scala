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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import ExpandedName._
import QName._

/**
 * Test case for [[eu.cdevreeze.yaidom.XmlStringUtils]].
 * 
 * The example test strings have been taken from http://docstore.mik.ua/orelly/xml/xmlnut/ch02_04.htm.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlStringUtilsTest extends Suite {

  @Test def testNameValidity() {
    import XmlStringUtils._

    expect(true) {
      isAllowedElementLocalName("Drivers_License_Number")
    }
    expect(false) {
      isAllowedElementLocalName("Driver's_License_Number")
    }

    expect(true) {
      isAllowedElementLocalName("month-day-year")
    }
    expect(false) {
      isAllowedElementLocalName("month/day/year")
    }

    expect(true) {
      isAllowedElementLocalName("first_name")
    }
    expect(false) {
      isAllowedElementLocalName("first name")
    }

    expect(true) {
      isAllowedElementLocalName("_4-lane")
    }
    expect(false) {
      isAllowedElementLocalName("4-lane")
    }

    expect(true) {
      isProbableXmlName("xmlns")
    }
    expect(false) {
      isAllowedElementLocalName("xmlns")
    }

    expect(true) {
      isProbableXmlName("cars:tire")
    }
    expect(false) {
      isAllowedElementLocalName("cars:tire")
    }

    expect(false) {
      isProbableXmlName("")
    }
    expect(false) {
      isProbableXmlName("<")
    }
    expect(false) {
      isProbableXmlName("&")
    }
  }
}
