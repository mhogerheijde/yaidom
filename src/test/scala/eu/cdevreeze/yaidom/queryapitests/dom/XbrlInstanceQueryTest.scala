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

package eu.cdevreeze.yaidom.queryapitests.dom

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceQueryTest
import eu.cdevreeze.yaidom.resolved
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XBRL instance query test case for DOM wrapper elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceQueryTest extends AbstractXbrlInstanceQueryTest {

  final type E = DomElem

  protected final val xbrlInstance: DomElem = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder()

    val is = classOf[XbrlInstanceQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/sample-xbrl-instance.xml")

    val domDoc = db.parse(is)

    DomElem(domDoc.getDocumentElement())
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(convert.DomConversions.convertToElem(elem.wrappedNode, xbrlInstance.scope))
}
