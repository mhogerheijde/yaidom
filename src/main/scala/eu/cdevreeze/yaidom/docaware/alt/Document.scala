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

package eu.cdevreeze.yaidom.docaware.alt

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ProcessingInstruction

/**
 * `Document`, containing a "docaware2" document element.
 *
 * @author Chris de Vreeze
 */
final class Document(
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends DocumentApi[Elem] with Immutable {

  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  require(documentElement.path == Path.Root, "The document element must have the root Path")

  def document: simple.Document =
    new simple.Document(uriOption, documentElement.elem, processingInstructions, comments)

  def uri: URI = documentElement.docUri

  def uriOption: Option[URI] = Some(uri)

  override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    documentElement = newRoot,
    processingInstructions = this.processingInstructions,
    comments = this.comments)
}

object Document {

  def apply(
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(documentElement, processingInstructions, comments)
  }

  def apply(docUri: URI, d: simple.Document): Document = {
    new Document(Elem(docUri, d.documentElement), d.processingInstructions, d.comments)
  }
}
