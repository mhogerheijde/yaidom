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
import javax.xml.stream.events._
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import StaxEventsToElemConverter._

/**
 * Converter from immutable.Seq[XMLEvent] to Elem
 *
 * @author Chris de Vreeze
 */
trait StaxEventsToElemConverter extends ConverterToElem[immutable.Seq[XMLEvent]] {

  def convertToElem(v: immutable.Seq[XMLEvent]): Elem = {
    val events: immutable.Seq[XMLEvent] = v.toList.dropWhile(ev => !ev.isStartElement)

    val eventsWithDepths: immutable.Seq[EventWithDepth] = {
      var depth = 0
      events.map(ev => ev match {
        case start: StartElement => depth += 1; new EventWithDepth(start, depth)
        case end: EndElement => val currDepth = depth; depth -= 1; new EventWithDepth(end, currDepth)
        case _ => new EventWithDepth(ev, depth)
      })
    }

    require(!eventsWithDepths.drop(1).isEmpty) // Cheaper than calling size for Lists

    val result = eventsToElem(eventsWithDepths, Scope.Empty)

    require(result.remainder.forall(ev => !ev.event.isStartElement && !ev.event.isEndElement))
    result.elem
  }

  private def eventsToElem(eventsWithDepths: immutable.Seq[EventWithDepth], parentScope: Scope): ElemResult = {
    require(!eventsWithDepths.drop(1).isEmpty) // Cheaper than calling size for Lists
    require(eventsWithDepths.head.event.isStartElement)

    val startElement: StartElement = eventsWithDepths.head.event.asStartElement

    val elem: Elem = eventToElem(startElement, parentScope)

    def startsWithMatchingEndElement(events: immutable.Seq[EventWithDepth]): Boolean = {
      if (events.isEmpty) false else {
        val head: EventWithDepth = events.head
        (head.depth == eventsWithDepths.head.depth) && (head.event.isEndElement)
      }
    }

    var remainingEvents = eventsWithDepths.drop(1)
    val children = mutable.Buffer[Node]()

    // Imperative code
    while (!remainingEvents.isEmpty && !startsWithMatchingEndElement(remainingEvents)) {
      remainingEvents.head match {
        case ev if ev.event.isStartElement => {
          // Recursive call (not tail-recursive, but recursion depth is rather limited)
          val result = eventsToElem(remainingEvents, elem.scope)
          val ch = result.elem
          children += ch
          remainingEvents = result.remainder
        }
        case ev if ev.event.isCharacters => {
          val charEv = ev.event.asCharacters
          val ch = if (charEv.isCData) eventToCData(charEv) else eventToText(charEv)
          children += ch
          remainingEvents = remainingEvents.drop(1)
        }
        case _ => remainingEvents = remainingEvents.drop(1)
      }
    }
    require(startsWithMatchingEndElement(remainingEvents))
    remainingEvents = remainingEvents.drop(1)

    val elemWithChildren: Elem = elem.withChildren(children.toIndexedSeq)
    new ElemResult(elemWithChildren, remainingEvents)
  }

  private def eventToText(event: Characters): Text = Text(event.getData)

  private def eventToCData(event: Characters): CData = CData(event.getData)

  private def eventToElem(startElement: StartElement, parentScope: Scope): Elem = {
    val declarations: Scope.Declarations = {
      val namespaces: List[Namespace] = startElement.getNamespaces.asScala.toList.collect({ case ns: Namespace => ns })
      require(namespaces.forall(ns => (ns.getNamespaceURI ne null) && (ns.getNamespaceURI != "")))
      // Namespace undeclaration not supported

      val declaredScope: Scope = new Scope(
        defaultNamespace = namespaces.filter(_.isDefaultNamespaceDeclaration).headOption.map(_.getNamespaceURI),
        prefixScope = namespaces.filterNot(_.isDefaultNamespaceDeclaration).map(ns => (ns.getPrefix -> ns.getNamespaceURI)).toMap)
      new Scope.Declarations(
        declared = declaredScope,
        defaultNamespaceUndeclared = false,
        undeclaredPrefixes = Set())
    }
    val currScope = parentScope.resolve(declarations)

    val elemExpandedName = ExpandedName.fromJavaQName(startElement.getName)
    val elemPrefix: Option[String] = ExpandedName.prefixFromJavaQName(startElement.getName)

    val currAttrs: Map[QName, String] = {
      val attributes: List[Attribute] = startElement.getAttributes.asScala.toList.collect({ case a: Attribute => a })
      attributes.map(a => {
        val prefix: Option[String] = ExpandedName.prefixFromJavaQName(a.getName)
        val name = ExpandedName.fromJavaQName(a.getName).toQName(prefix)
        (name -> a.getValue)
      }).toMap
    }

    // Line and column numbers can be retrieved from startElement.getLocation, but are ignored here

    Elem(elemExpandedName.toQName(elemPrefix), currAttrs, currScope, Nil.toIndexedSeq)
  }

  // TODO EntityRef, ProcessingInstruction (but those StAX events are only fired if document contains DTD)
}

object StaxEventsToElemConverter {

  private final class EventWithDepth(val event: XMLEvent, val depth: Int) extends Immutable

  private final class ElemResult(val elem: Elem, val remainder: immutable.Seq[EventWithDepth]) extends Immutable
}