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

package eu.cdevreeze.yaidom.queryapitests.docaware

import java.net.URI

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.docaware
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.simple

/**
 * XML Base test case for docaware Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends Suite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  @Test def testXmlBase(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    testXmlBase(docawareDoc.documentElement)
  }

  @Test def testXmlBase2(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    val elem = docaware.Elem(docUri, doc.documentElement)

    testXmlBase(elem)
  }

  @Test def testXmlBase3(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    val elem = docaware.Elem(docUri, new URI("http://bogusBaseUri"), doc.documentElement)

    testXmlBase(elem)
  }

  @Test def testXmlBase4(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    val path = docawareDoc.documentElement.findElem(_.resolvedName == EName("olist")).get.path
    val elem = docaware.Elem(docUri, doc.documentElement, path)

    testXmlBaseOfNonRootElem(elem)
  }

  @Test def testXmlBase5(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    val path = docawareDoc.documentElement.findElem(_.resolvedName == EName("olist")).get.path
    val elem = docaware.Elem(docUri, docawareDoc.documentElement.baseUri, doc.documentElement, path)

    testXmlBaseOfNonRootElem(elem)
  }

  @Test def testXmlBase6(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    val foundElem = docawareDoc.documentElement.findElem(_.resolvedName == EName("olist")).get
    // Now make the olist element root itself
    val elem = docaware.Elem(docUri, docawareDoc.documentElement.baseUri, foundElem.elem, Path.Root)

    testXmlBaseOfNonRootElem(elem)
  }

  @Test def testOtherXmlBase(): Unit = {
    val elem = testElem

    assertResult(new URI("http://example.org/wine/")) {
      elem.baseUri
    }
    assertResult(new URI("http://example.org/wine/rosé")) {
      elem.getChildElem(_.localName == "e2").baseUri
    }
  }

  private def testXmlBase(elem: docaware.Elem): Unit = {
    assertResult(2) {
      elem.filterElemsOrSelf(e => e.attributeOption(XmlBaseEName).isDefined).size
    }
    assertResult(new URI("http://example.org/today/")) {
      elem.baseUri
    }
    assertResult(Set(new URI("http://example.org/hotpicks/"))) {
      elem.filterElems(EName("olist")).map(_.baseUri).toSet
    }
    assertResult(Set(
      new URI("http://example.org/today/new.xml"),
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          e.baseUri.resolve(href)
        }
      uris.toSet
    }
  }

  private def testXmlBaseOfNonRootElem(elem: docaware.Elem): Unit = {
    require(elem.resolvedName == EName("olist"))

    assertResult(new URI("http://example.org/hotpicks/")) {
      elem.baseUri
    }

    assertResult(Set(
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          e.baseUri.resolve(href)
        }
      uris.toSet
    }
  }

  private def testElem: docaware.Elem = {
    import simple.Node._

    val scope = Scope.Empty

    val elm =
      emptyElem(QName("e1"), Vector(QName("xml:base") -> "http://example.org/wine/"), scope).
        plusChild(emptyElem(QName("e2"), Vector(QName("xml:base") -> "rosé"), scope))

    docaware.Elem(new URI(""), elm)
  }
}