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

package eu.cdevreeze.yaidom.queryapitests.scalaxml

import java.{ util => jutil }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.queryapitests.AbstractRobustQueryTest
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlNode

/**
 * Query test case for Scala XML wrapper elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class RobustQueryTest extends AbstractRobustQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.scalaxml")

  final type E = ScalaXmlElem

  protected final val contactsElem: ScalaXmlElem = {
    // See http://richard.dallaway.com/2013-02-06

    val resolvingXmlLoader = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def adapter: scala.xml.parsing.FactoryAdapter = new scala.xml.parsing.NoBindingFactoryAdapter() {
        override def resolveEntity(publicId: String, systemId: String): org.xml.sax.InputSource = {
          null
        }
      }
    }

    val is = classOf[RobustQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/contacts.xml")

    val root: ScalaXmlElem = ScalaXmlNode.wrapElement(resolvingXmlLoader.load(is))
    root
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(ScalaXmlConversions.convertToElem(elem.wrappedNode))
}
