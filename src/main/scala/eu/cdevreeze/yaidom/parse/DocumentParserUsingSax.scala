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
package parse

import java.{ io => jio }
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.ext.LexicalHandler

/**
 * SAX-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming a trait `MyEntityResolver`, which extends `EntityResolver`,
 * and a trait `MyErrorHandler`, which extends `ErrorHandler`:
 * {{{
 * val parser = DocumentParserUsingSax.newInstance(
 *   SAXParserFactory.newInstance,
 *   () => new DefaultElemProducingSaxHandler with MyEntityResolver with MyErrorHandler
 * )
 * }}}
 *
 * A custom `EntityResolver` could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds):
 * {{{
 * trait MyEntityResolver extends EntityResolver {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 *
 * A `DocumentParserUsingSax` instance can be re-used multiple times, from the same thread.
 * If the `SAXParserFactory` is thread-safe, it can even be re-used from multiple threads.
 */
final class DocumentParserUsingSax(
  val parserFactory: SAXParserFactory,
  val parserCreator: SAXParserFactory => SAXParser,
  val handlerCreator: () => ElemProducingSaxHandler) extends DocumentParser {

  /**
   * Parses the input stream into a yaidom `Document`. Closes the input stream afterwards.
   *
   * If the created `DefaultHandler` is a `LexicalHandler`, this `LexicalHandler` is registered. In practice all SAX parsers
   * should support LexicalHandlers.
   */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val sp: SAXParser = parserCreator(parserFactory)
      val handler = handlerCreator()

      if (handler.isInstanceOf[LexicalHandler]) {
        // Property "http://xml.org/sax/properties/lexical-handler" registers a LexicalHandler. See the corresponding API documentation.
        // It is assumed here that in practice all SAX parsers support LexicalHandlers.
        sp.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", handler)
      }

      sp.parse(inputStream, handler)

      val doc: Document = handler.resultingDocument
      doc
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingSax {

  /** Returns a new instance. Same as `newInstance(SAXParserFactory.newInstance)`. */
  def newInstance(): DocumentParserUsingSax = {
    val spf = SAXParserFactory.newInstance
    newInstance(spf)
  }

  /** Returns `newInstance(parserFactory, new DefaultElemProducingSaxHandler {})`. */
  def newInstance(parserFactory: SAXParserFactory): DocumentParserUsingSax =
    newInstance(parserFactory, () => new DefaultElemProducingSaxHandler {})

  /**
   * Invokes the 3-arg `newInstance` method on `parserFactory`, a `SAXParserFactory => SAXParser` "SAX parser creator", and
   * `handlerCreator`. The "SAX parser creator" invokes `parserFactory.newSAXParser()`.
   */
  def newInstance(parserFactory: SAXParserFactory, handlerCreator: () => ElemProducingSaxHandler): DocumentParserUsingSax = {
    newInstance(
      parserFactory = parserFactory,
      parserCreator = { (spf: SAXParserFactory) =>
        val parser = spf.newSAXParser()
        parser
      },
      handlerCreator = handlerCreator)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    parserFactory: SAXParserFactory,
    parserCreator: SAXParserFactory => SAXParser,
    handlerCreator: () => ElemProducingSaxHandler): DocumentParserUsingSax = {

    new DocumentParserUsingSax(parserFactory, parserCreator, handlerCreator)
  }
}
