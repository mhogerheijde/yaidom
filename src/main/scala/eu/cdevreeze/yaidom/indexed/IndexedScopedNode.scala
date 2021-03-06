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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.IndexedScopedElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved.ResolvedNodes

object IndexedScopedNode {

  /**
   * Node super-type of scoped Clark elements.
   *
   * @author Chris de Vreeze
   */
  sealed trait Node extends Nodes.Node

  sealed trait CanBeDocumentChild extends Node with Nodes.CanBeDocumentChild

  /**
   * Indexed Scoped element. Like `IndexedClarkElem` but instead of being and indexing
   * a `ClarkElemApi`, it is and indexes a `ScopedElemApi`. Other than that, see the
   * documentation for `IndexedClarkElem`.
   *
   * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
   * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
   *
   * @author Chris de Vreeze
   */
  final class Elem[U <: ScopedElemApi.Aux[U]] private[IndexedScopedNode] (
    docUriOption: Option[URI],
    underlyingRootElem: U,
    path: Path,
    underlyingElem: U) extends AbstractIndexedClarkElem(docUriOption, underlyingRootElem, path, underlyingElem) with CanBeDocumentChild with IndexedScopedElemApi with ScopedElemLike with Nodes.Elem {

    type ThisElemApi = Elem[U]

    type ThisElem = Elem[U]

    def thisElem: ThisElem = this

    final def findAllChildElems: immutable.IndexedSeq[ThisElem] = {
      underlyingElem.findAllChildElemsWithPathEntries map {
        case (e, entry) =>
          new Elem(docUriOption, underlyingRootElem, path.append(entry), e)
      }
    }

    final def qname: QName = underlyingElem.qname

    final def attributes: immutable.IndexedSeq[(QName, String)] = underlyingElem.attributes.toIndexedSeq

    final def scope: Scope = underlyingElem.scope

    final override def equals(obj: Any): Boolean = obj match {
      case other: Elem[U] =>
        (other.docUriOption == this.docUriOption) && (other.underlyingRootElem == this.underlyingRootElem) &&
          (other.path == this.path) && (other.underlyingElem == this.underlyingElem)
      case _ => false
    }

    final override def hashCode: Int = (docUriOption, underlyingRootElem, path, underlyingElem).hashCode

    final def rootElem: ThisElem = {
      new Elem[U](docUriOption, underlyingRootElem, Path.Empty, underlyingRootElem)
    }

    final def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem] = {
      val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

      assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(resultOption.get.last == thisElem)

      resultOption.get
    }

    final def namespaces: Declarations = {
      val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
      parentScope.relativize(this.scope)
    }
  }

  final case class Text(text: String, isCData: Boolean) extends Node with Nodes.Text {
    require(text ne null)
    if (isCData) require(!text.containsSlice("]]>"))

    /** Returns `text.trim`. */
    def trimmedText: String = text.trim

    /** Returns `XmlStringUtils.normalizeString(text)` .*/
    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final case class ProcessingInstruction(target: String, data: String) extends CanBeDocumentChild with Nodes.ProcessingInstruction {
    require(target ne null)
    require(data ne null)
  }

  /**
   * An entity reference. For example:
   * {{{
   * &hello;
   * }}}
   * We obtain this entity reference as follows:
   * {{{
   * EntityRef("hello")
   * }}}
   */
  final case class EntityRef(entity: String) extends Node with Nodes.EntityRef {
    require(entity ne null)
  }

  final case class Comment(text: String) extends CanBeDocumentChild with Nodes.Comment {
    require(text ne null)
  }

  object Elem {

    def apply[U <: ScopedElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U, path: Path): Elem[U] = {
      new Elem[U](docUriOption, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
    }

    def apply[U <: ScopedElemApi.Aux[U]](docUri: URI, underlyingRootElem: U, path: Path): Elem[U] = {
      apply(Some(docUri), underlyingRootElem, path)
    }

    def apply[U <: ScopedElemApi.Aux[U]](underlyingRootElem: U, path: Path): Elem[U] = {
      new Elem[U](None, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
    }

    def apply[U <: ScopedElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U): Elem[U] = {
      apply(docUriOption, underlyingRootElem, Path.Empty)
    }

    def apply[U <: ScopedElemApi.Aux[U]](docUri: URI, underlyingRootElem: U): Elem[U] = {
      apply(Some(docUri), underlyingRootElem, Path.Empty)
    }

    def apply[U <: ScopedElemApi.Aux[U]](underlyingRootElem: U): Elem[U] = {
      apply(None, underlyingRootElem, Path.Empty)
    }

    /**
     * Returns the child nodes of the given element, provided the underlying element knows its child nodes too
     * (which is assured by the type restriction on the underlying element).
     */
    def getChildren[U <: ScopedElemApi.Aux[U] with ResolvedNodes.Elem](elem: Elem[U]): immutable.IndexedSeq[Node] = {
      val childElems = elem.findAllChildElems
      var childElemIdx = 0

      val children =
        elem.underlyingElem.children map {
          case che: ResolvedNodes.Elem =>
            val e = childElems(childElemIdx)
            childElemIdx += 1
            e
          case ch: ResolvedNodes.Text =>
            Text(ch.text, false)
          case ch: Nodes.Comment =>
            Comment(ch.text)
          case ch: Nodes.ProcessingInstruction =>
            ProcessingInstruction(ch.target, ch.data)
          case ch: Nodes.EntityRef =>
            EntityRef(ch.entity)
        }

      assert(childElemIdx == childElems.size)

      children
    }
  }
}
