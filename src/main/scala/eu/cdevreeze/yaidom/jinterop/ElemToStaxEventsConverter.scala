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
package jinterop

import java.{ util => jutil }
import javax.xml.XMLConstants
import javax.xml.stream._
import javax.xml.stream.events.{ ProcessingInstruction => _, Comment => _, _ }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import ElemToStaxEventsConverter._

/**
 * Converter from [[eu.cdevreeze.yaidom.Elem]] to immutable.IndexedSeq[XMLEvent]
 *
 * @author Chris de Vreeze
 */
trait ElemToStaxEventsConverter extends ElemConverter[XmlEventsProducer] with DocumentConverter[XmlEventsProducer] {

  def convertDocument(doc: Document): XmlEventsProducer = {
    { (xmlEventFactory: XMLEventFactory) =>
      val startDocument = xmlEventFactory.createStartDocument
      // For the line separator, see for example http://xerces.apache.org/xerces-j/apiDocs/org/apache/xml/serialize/OutputFormat.html#setLineSeparator(java.lang.String).
      val newline = xmlEventFactory.createCharacters("\n")
      val piEvents: immutable.IndexedSeq[XMLEvent] =
        doc.processingInstructions flatMap { pi => convertProcessingInstruction(pi, xmlEventFactory) }
      val commentEvents: immutable.IndexedSeq[XMLEvent] =
        doc.comments flatMap { com => convertComment(com, xmlEventFactory) }
      val docElmEvents = convertElem(doc.documentElement, xmlEventFactory, Scope.Empty)
      val endDocument = xmlEventFactory.createEndDocument

      immutable.IndexedSeq(startDocument, newline) ++ piEvents ++ commentEvents ++ docElmEvents ++ immutable.IndexedSeq(endDocument)
    }
  }

  def convertElem(elm: Elem): XmlEventsProducer = {
    { (xmlEventFactory: XMLEventFactory) =>
      val startDocument = xmlEventFactory.createStartDocument
      // For the line separator, see for example http://xerces.apache.org/xerces-j/apiDocs/org/apache/xml/serialize/OutputFormat.html#setLineSeparator(java.lang.String).
      val newline = xmlEventFactory.createCharacters("\n")
      val nonDocEvents = convertElem(elm, xmlEventFactory, Scope.Empty)
      val endDocument = xmlEventFactory.createEndDocument

      immutable.IndexedSeq(startDocument, newline) ++ nonDocEvents ++ immutable.IndexedSeq(endDocument)
    }
  }

  private def convertNode(node: Node, xmlEventFactory: XMLEventFactory, parentScope: Scope): immutable.IndexedSeq[XMLEvent] = {
    node match {
      case e: Elem => convertElem(e, xmlEventFactory, parentScope)
      case t: Text if t.isCData => convertCData(t, xmlEventFactory)
      case t: Text => require(!t.isCData); convertText(t, xmlEventFactory)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, xmlEventFactory)
      // Difficult to convert yaidom EntityRef to StAX EntityReference, because of missing declaration
      case er: EntityRef => immutable.IndexedSeq[XMLEvent]()
      case c: Comment => convertComment(c, xmlEventFactory)
    }
  }

  private def convertElem(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): immutable.IndexedSeq[XMLEvent] = {
    // Not tail-recursive, but the recursion depth should be limited

    val startEvent: XMLEvent = createStartElement(elm, xmlEventFactory, parentScope)
    val childEvents: immutable.IndexedSeq[XMLEvent] = elm.children flatMap { ch => convertNode(ch, xmlEventFactory, elm.scope) }
    val endEvent: XMLEvent = createEndElement(elm, xmlEventFactory, parentScope)

    immutable.IndexedSeq(startEvent) ++ childEvents ++ immutable.IndexedSeq(endEvent)
  }

  private def convertCData(cdata: Text, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createCData(cdata.text)
    immutable.IndexedSeq(event)
  }

  private def convertText(text: Text, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createCharacters(text.text)
    immutable.IndexedSeq(event)
  }

  private def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {

    val event = xmlEventFactory.createProcessingInstruction(processingInstruction.target, processingInstruction.data)
    immutable.IndexedSeq(event)
  }

  private def convertComment(comment: Comment, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createComment(comment.text)
    immutable.IndexedSeq(event)
  }

  private def createStartElement(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): StartElement = {
    val namespaceDeclarations: Scope.Declarations = parentScope.relativize(elm.scope)

    val javaQName = elm.resolvedName.toJavaQName(elm.qname.prefixOption)

    val attributeIterable: Iterable[Attribute] = {
      val result = elm.attributes map { kv =>
        val attrQName = kv._1
        val value = kv._2
        val attrExpandedName = elm.attributeScope.resolveQName(attrQName).getOrElse(sys.error(
          "Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attrQName, elm.attributeScope)))
        val attrJavaQName = attrExpandedName.toJavaQName(attrQName.prefixOption)

        (attrQName -> xmlEventFactory.createAttribute(attrJavaQName, value))
      }
      result.values
    }

    val attributes: jutil.Iterator[Attribute] = new jutil.ArrayList[Attribute](attributeIterable.toBuffer.asJava).iterator

    val namespaceIterable: Iterable[Namespace] = {
      val result = namespaceDeclarations.toMap map { kv =>
        val prefix = kv._1
        val nsUri = kv._2

        if ((prefix eq null) || (prefix == "")) {
          (prefix -> xmlEventFactory.createNamespace(nsUri))
        } else {
          (prefix -> xmlEventFactory.createNamespace(prefix, nsUri))
        }
      }
      result.values
    }

    val namespaces: jutil.Iterator[Namespace] = new jutil.ArrayList[Namespace](namespaceIterable.toBuffer.asJava).iterator

    xmlEventFactory.createStartElement(javaQName, attributes, namespaces)
  }

  private def createEndElement(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): EndElement = {
    val namespaceDeclarations: Scope.Declarations = parentScope.relativize(elm.scope)

    val javaQName = elm.resolvedName.toJavaQName(elm.qname.prefixOption)

    val namespaceOutOfScopeIterable: Iterable[Namespace] = {
      val result = namespaceDeclarations.toMap map { kv =>
        val prefix = kv._1
        val nsUri = kv._2

        if ((prefix eq null) || (prefix == "")) {
          (prefix -> xmlEventFactory.createNamespace(nsUri))
        } else {
          (prefix -> xmlEventFactory.createNamespace(prefix, nsUri))
        }
      }
      result.values
    }

    val namespacesOutOfScope: jutil.Iterator[Namespace] =
      new jutil.ArrayList[Namespace](namespaceOutOfScopeIterable.toBuffer.asJava).iterator

    xmlEventFactory.createEndElement(javaQName, namespacesOutOfScope)
  }
}

object ElemToStaxEventsConverter {

  type XmlEventsProducer = (XMLEventFactory => immutable.IndexedSeq[XMLEvent])
}
