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

package eu.cdevreeze.yaidom.simple

import java.net.URI

import scala.collection.immutable

import NodeBuilder.fromComment
import NodeBuilder.fromElem
import NodeBuilder.fromProcessingInstruction
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi

/**
 * Builder of a yaidom Document. Called `DocBuilder` instead of `DocumentBuilder`, because often a JAXP `DocumentBuilder` is in scope too.
 * A `DocBuilder` is itself not a `NodeBuilder`.
 *
 * A `DocBuilder` is constructed from an optional URI, an optional XML declaration, a document element (as `ElemBuilder`), top-level processing
 * instruction builders, if any, and top-level comment builders, if any.
 *
 * @author Chris de Vreeze
 */
@SerialVersionUID(1L)
final class DocBuilder(
  val uriOption: Option[URI],
  val xmlDeclarationOption: Option[XmlDeclaration],
  val documentElement: ElemBuilder,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder],
  val comments: immutable.IndexedSeq[CommentBuilder]) extends DocumentApi[ElemBuilder] with Immutable with Serializable { self =>

  require(uriOption ne null)
  require(xmlDeclarationOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  /**
   * Returns the child node builders, that is, the "document element" and the top-level processing instructions and comments,
   * if any.
   */
  def children: immutable.IndexedSeq[NodeBuilder] =
    (processingInstructions ++ comments) :+ documentElement

  /**
   * Creates a [[eu.cdevreeze.yaidom.simple.Document]] from this document builder.
   */
  def build(): Document = {
    val parentScope = Scope.Empty

    Document(
      uriOption = uriOption,
      xmlDeclarationOption = xmlDeclarationOption,
      documentElement = documentElement.build(parentScope),
      processingInstructions = processingInstructions map { (pi: ProcessingInstructionBuilder) => pi.build(parentScope) },
      comments = comments map { (c: CommentBuilder) => c.build(parentScope) })
  }

  /** Returns the tree representation. See the corresponding method in [[eu.cdevreeze.yaidom.simple.Document]]. */
  final def toTreeRepr(): String = build().toTreeRepr()

  /** Returns `toTreeRepr` */
  final override def toString: String = toTreeRepr()
}

object DocBuilder {

  /**
   * Creates a document builder from a document.
   */
  def fromDocument(doc: Document): DocBuilder = {
    import NodeBuilder._

    val parentScope = Scope.Empty

    new DocBuilder(
      uriOption = doc.uriOption,
      xmlDeclarationOption = doc.xmlDeclarationOption,
      documentElement = fromElem(doc.documentElement)(parentScope),
      processingInstructions = doc.processingInstructions collect { case pi: ProcessingInstruction => fromProcessingInstruction(pi) },
      comments = doc.comments collect { case c => fromComment(c) })
  }

  def document(
    uriOption: Option[String] = None,
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: ElemBuilder,
    processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder] = Vector(),
    comments: immutable.IndexedSeq[CommentBuilder] = Vector()): DocBuilder = {

    new DocBuilder(
      uriOption map { uriString => new URI(uriString) },
      xmlDeclarationOption,
      documentElement,
      processingInstructions,
      comments)
  }
}
