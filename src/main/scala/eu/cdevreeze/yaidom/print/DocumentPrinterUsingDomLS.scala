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

package eu.cdevreeze.yaidom.print

import java.{ io => jio }

import scala.Vector

import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSOutput
import org.w3c.dom.ls.LSSerializer

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import DocumentPrinterUsingDomLS.DocumentProducer

/**
 * DOM-LS-based `Document` printer.
 *
 * To pretty-print a `Document`, create a `DocumentPrinterUsingDomLS` instance as follows:
 * {{{
 * val printer = DocumentPrinterUsingDomLS.newInstance() withSerializerCreator { domImpl =>
 *   val writer = domImpl.createLSSerializer()
 *   writer.getDomConfig.setParameter("format-pretty-print", java.lang.Boolean.TRUE)
 *   writer
 * }
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingDomLS`, but adapts the `print` method.
 * This would make it possible to adapt the serialization, for example.
 *
 * A `DocumentPrinterUsingDomLS` instance can be re-used multiple times, from the same thread.
 * If the `DocumentBuilderFactory` and `DOMImplementationLS` are thread-safe, it can even be re-used from multiple threads.
 * Typically a `DocumentBuilderFactory` or `DOMImplementationLS` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `DocumentBuilderFactory` and `DOMImplementationLS` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentPrinterUsingDomLS(
  val docBuilderFactory: DocumentBuilderFactory,
  val docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
  val domImplementation: DOMImplementationLS,
  val serializerCreator: DOMImplementationLS => LSSerializer,
  val documentConverter: DocumentConverter[DocumentProducer]) extends AbstractDocumentPrinter { self =>

  /**
   * Returns an adapted copy having the passed DocumentConverter. This method makes it possible to use an adapted
   * document converter, which may be needed depending on the JAXP implementation used.
   */
  def withDocumentConverter(newDocumentConverter: DocumentConverter[DocumentProducer]): DocumentPrinterUsingDomLS = {
    new DocumentPrinterUsingDomLS(
      docBuilderFactory,
      docBuilderCreator,
      domImplementation,
      serializerCreator,
      newDocumentConverter)
  }

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = documentConverter.convertDocument(doc)(docBuilder.newDocument)

    val serializer: LSSerializer = serializerCreator(domImplementation)

    try {
      val output: LSOutput = domImplementation.createLSOutput()
      output.setEncoding(encoding)
      output.setByteStream(outputStream)

      val ok = serializer.write(domDocument, output)
      require(ok, s"Expected successful serialization of Document ${doc.documentElement.withChildren(Vector(Text(" ... ", false)))}")
    } finally {
      outputStream.close()
    }
  }

  def print(doc: Document): String = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = documentConverter.convertDocument(doc)(docBuilder.newDocument)

    val serializer: LSSerializer = serializerCreator(domImplementation)

    val output: LSOutput = domImplementation.createLSOutput()
    val sw = new jio.StringWriter
    output.setEncoding("utf-8")
    output.setCharacterStream(sw)

    val ok = serializer.write(domDocument, output)
    require(ok, s"Expected successful serialization of Document ${doc.documentElement.withChildren(Vector(Text(" ... ", false)))}")

    val result = sw.toString
    result
  }

  def omittingXmlDeclaration: DocumentPrinterUsingDomLS = {
    val newSerializerCreator = { domImpl: DOMImplementationLS =>
      val serializer = self.serializerCreator(domImpl)
      val domConfig = serializer.getDomConfig
      domConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      serializer
    }

    withSerializerCreator(newSerializerCreator)
  }

  def withSerializerCreator(newSerializerCreator: DOMImplementationLS => LSSerializer): DocumentPrinterUsingDomLS = {
    new DocumentPrinterUsingDomLS(
      docBuilderFactory,
      docBuilderCreator,
      domImplementation,
      newSerializerCreator,
      documentConverter)
  }
}

object DocumentPrinterUsingDomLS {

  /** Producer of a DOM `Document`, given the DOM `Document` as factory of DOM objects */
  type DocumentProducer = (org.w3c.dom.Document => org.w3c.dom.Document)

  /** Returns `newInstance(DocumentBuilderFactory.newInstance, domImplLS)`, for an appropriate `DOMImplementationLS` */
  def newInstance(): DocumentPrinterUsingDomLS = {
    val registry = DOMImplementationRegistry.newInstance
    val domImpl = registry.getDOMImplementation("LS 3.0")
    require(domImpl ne null, "Expected non-null DOM Implementation for feature 'LS 3.0'")
    require(domImpl.hasFeature("LS", "3.0"), "Expected DOM Implementation to have feature 'LS 3.0'")
    require(domImpl.isInstanceOf[DOMImplementationLS], "Expected DOM Implementation of type DOMImplementationLS")
    val domImplLS = domImpl.asInstanceOf[DOMImplementationLS]

    val docBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    newInstance(docBuilderFactory, domImplLS)
  }

  /** Invokes the 4-arg `newInstance` method, with trivial "docBuilderCreator" and "serializerCreator" */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    domImplementation: DOMImplementationLS): DocumentPrinterUsingDomLS = {

    newInstance(
      docBuilderFactory,
      { dbf => dbf.newDocumentBuilder() },
      domImplementation,
      { domImpl => domImpl.createLSSerializer() })
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
    domImplementation: DOMImplementationLS,
    serializerCreator: DOMImplementationLS => LSSerializer): DocumentPrinterUsingDomLS = {

    new DocumentPrinterUsingDomLS(
      docBuilderFactory,
      docBuilderCreator,
      domImplementation,
      serializerCreator,
      DomConversions)
  }
}
