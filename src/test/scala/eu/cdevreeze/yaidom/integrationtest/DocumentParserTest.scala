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

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import scala.io.Codec

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlDocument
import eu.cdevreeze.yaidom.simple.DocBuilder
import eu.cdevreeze.yaidom.simple.Document
import javax.xml.parsers.DocumentBuilderFactory

/**
 * DocumentParser test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DocumentParserTest extends Suite {

  @Test def testParseWithEndingCommentsUsingSax(): Unit = {
    val parser = DocumentParserUsingSax.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseWithEndingCommentsUsingStax(): Unit = {
    val parser = DocumentParserUsingStax.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseWithEndingCommentsUsingDom(): Unit = {
    val parser = DocumentParserUsingDom.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseWithEndingCommentsUsingDomLS(): Unit = {
    val parser = DocumentParserUsingDomLS.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseXml11UsingSax(): Unit = {
    val parser = DocumentParserUsingSax.newInstance

    doTestParseXml11(parser)
  }

  @Test def testParseXml11UsingStax(): Unit = {
    val parser = DocumentParserUsingStax.newInstance

    doTestParseXml11(parser)
  }

  @Test def testParseXml11UsingDom(): Unit = {
    val parser = DocumentParserUsingDom.newInstance

    doTestParseXml11(parser)
  }

  @Test def testParseXml11UsingDomLS(): Unit = {
    val parser = DocumentParserUsingDomLS.newInstance

    doTestParseXml11(parser)
  }

  @Test def testParseScalaXmlWithEndingComments(): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |<!-- Bogus comment at the end -->
         |""".stripMargin.trim

    val doc = ScalaXmlDocument(
      scala.xml.parsing.ConstructingParser.fromSource(scala.io.Source.fromString(xml), true).document)

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the end")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.documentElement, doc.comments.head)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  @Test def testParseScalaXmlWithStartingComments(): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="iso-8859-1" standalone="no"?>
         |<!-- Bogus comment at the beginning -->
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val doc = ScalaXmlDocument(
      scala.xml.parsing.ConstructingParser.fromSource(scala.io.Source.fromString(xml), true).document)

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the beginning")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.comments.head, doc.documentElement)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Charset.forName("iso-8859-1"))) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(false)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  @Test def testParseDomWithEndingComments(): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |<!-- Bogus comment at the end -->
         |""".stripMargin.trim

    val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = DomDocument(
      db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))))

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the end")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.documentElement, doc.comments.head)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  @Test def testParseDomWithStartingComments(): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="iso-8859-1" standalone="no"?>
         |<!-- Bogus comment at the beginning -->
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = DomDocument(
      db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))))

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the beginning")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.comments.head, doc.documentElement)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Charset.forName("iso-8859-1"))) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(false)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  private def doTestParseWithEndingComments(docParser: DocumentParser): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |<!-- Bogus comment at the end -->
         |""".stripMargin.trim

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the end")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.documentElement, doc.comments.head)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }

    val doc2 = DocBuilder.fromDocument(doc).build()

    val xmlDeclOption2 = doc2.xmlDeclarationOption

    assertResult(xmlDeclOption) {
      xmlDeclOption2
    }

    assertResult(List(doc2.documentElement, doc2.comments.head)) {
      doc2.children
    }
  }

  private def doTestParseXml11(docParser: DocumentParser): Unit = {
    val xml =
      """|<?xml version="1.1" encoding="iso-8859-1" standalone="no"?>
         |<!-- Bogus comment at the beginning -->
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the beginning")) {
      doc.comments.map(_.text.trim)
    }
    assertResult(List(doc.comments.head, doc.documentElement)) {
      doc.children
    }

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.1")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Charset.forName("iso-8859-1"))) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(false)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }

    val doc2 = DocBuilder.fromDocument(doc).build()

    val xmlDeclOption2 = doc2.xmlDeclarationOption

    assertResult(xmlDeclOption) {
      xmlDeclOption2
    }

    assertResult(List(doc2.comments.head, doc2.documentElement)) {
      doc2.children
    }
  }
}
