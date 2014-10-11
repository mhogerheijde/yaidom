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

package eu.cdevreeze.yaidom.defaultelem

import java.net.URI

import scala.Vector
import scala.collection.immutable
import scala.util.parsing.combinator.JavaTokenParsers

import org.apache.commons.lang3.StringEscapeUtils

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName

/**
 * Generator for parsers of "tree representation" expressions. The results from successful parses are `NodeBuilder` and
 * `DocBuilder` instances.
 *
 * The "tree representation" expressions are themselves valid Scala code!
 *
 * Note: Parsing large "tree representations" is very slow.
 *
 * @author Chris de Vreeze
 */
object TreeReprParsers extends JavaTokenParsers {

  // Using a fixed version of stringLiteral. See https://issues.scala-lang.org/browse/SI-4138.

  def stringLiteral2: Parser[String] =
    ("\"" + """([^"\p{Cntrl}\\]|\\[\\/bfnrt"]|\\u[a-fA-F0-9]{4})*""" + "\"").r

  def concatenatedStringLiterals: Parser[String] = {
    // Also first unwraps the String literals
    repsep(stringLiteral2, "+") ^^ { xs => xs.map(s => unwrapStringLiteral(s)).mkString }
  }

  // DocBuilder

  def document: Parser[DocBuilder] =
    "document" ~> "(" ~> documentContent <~ ")" ^^ {
      case (uriOpt, elm, optPis, optComments) =>
        val pis = optPis.getOrElse(Vector[ProcessingInstructionBuilder]())
        val comments = optComments.getOrElse(Vector[CommentBuilder]())
        new DocBuilder(uriOpt, elm, pis, comments)
    }

  type DocumentContent = (Option[URI], ElemBuilder, Option[Vector[ProcessingInstructionBuilder]], Option[Vector[CommentBuilder]])

  def documentContent: Parser[DocumentContent] = {
    (docUriOptionPart <~ ",") ~ documentElementPart ~ opt("," ~> docProcessingInstructionsPart) ~ opt("," ~> docCommentsPart) ^^ {
      case uriPart ~ elmPart ~ piPart ~ commentsPart => (uriPart, elmPart, piPart, commentsPart)
    }
  }

  def docUriOptionPart: Parser[Option[URI]] = {
    "uriOption" ~> "=" ~> docUriOption ^^ { x => x map { s => new URI(s) } }
  }

  def docUriOption: Parser[Option[String]] = (docUriEmpty | docUriDefined)

  def docUriEmpty: Parser[Option[String]] =
    "None" ^^ { x => None }

  def docUriDefined: Parser[Option[String]] =
    "Some" ~> "(" ~> stringLiteral2 <~ ")" ^^ { x => Some(unwrapStringLiteral(x)) }

  def documentElementPart: Parser[ElemBuilder] =
    "documentElement" ~> "=" ~> element

  def docProcessingInstructionsPart: Parser[Vector[ProcessingInstructionBuilder]] =
    "processingInstructions" ~> "=" ~> docProcessingInstructions

  def docProcessingInstructions: Parser[Vector[ProcessingInstructionBuilder]] =
    "Vector" ~> "(" ~> repsep(processingInstruction, ",") <~ ")" ^^ { xs => Vector(xs: _*) }

  def docCommentsPart: Parser[Vector[CommentBuilder]] =
    "comments" ~> "=" ~> docComments

  def docComments: Parser[Vector[CommentBuilder]] =
    "Vector" ~> "(" ~> repsep(comment, ",") <~ ")" ^^ { xs => Vector(xs: _*) }

  // ElemBuilder

  def element: Parser[ElemBuilder] = {
    "elem" ~> "(" ~> elemContent <~ ")" ^^
      { case (qname, attrs, namespaces, children) => new ElemBuilder(qname, attrs, namespaces, children) }
  }

  def elemContent: Parser[(QName, immutable.IndexedSeq[(QName, String)], Declarations, Vector[NodeBuilder])] = {
    qnamePart ~ opt("," ~> attributesPart) ~ opt("," ~> namespacesPart) ~ opt("," ~> elemChildrenPart) ^^
      {
        case (qn ~ optAttrs ~ optNs ~ optChildren) =>
          val attrs = optAttrs.getOrElse(Vector[(QName, String)]())
          val ns = optNs.getOrElse(Declarations.Empty)
          val children = optChildren.getOrElse(Vector[NodeBuilder]())
          (qn, attrs, ns, children)
      }
  }

  def qnamePart: Parser[QName] = "qname" ~> "=" ~> qname

  def qname: Parser[QName] =
    "QName" ~> "(" ~> stringLiteral2 <~ ")" ^^ { x => QName.parse(unwrapStringLiteral(x)) }

  def attributesPart: Parser[immutable.IndexedSeq[(QName, String)]] =
    "attributes" ~> "=" ~> attributes

  def attributes: Parser[immutable.IndexedSeq[(QName, String)]] =
    "Vector" ~> "(" ~> repsep(attribute, ",") <~ ")" ^^ { xs => xs.toIndexedSeq }

  def attribute: Parser[(QName, String)] =
    qname ~ "->" ~ stringLiteral2 ^^ { case qn ~ "->" ~ v => (qn, unwrapStringLiteral(v)) }

  def namespacesPart: Parser[Declarations] =
    "namespaces" ~> "=" ~> namespaces ^^ { xs => Declarations.from(xs) }

  def namespaces: Parser[Map[String, String]] =
    "Declarations.from" ~> "(" ~> repsep(namespace, ",") <~ ")" ^^ { xs => xs.toMap }

  def namespace: Parser[(String, String)] =
    stringLiteral2 ~ "->" ~ stringLiteral2 ^^ {
      case prefix ~ "->" ~ uri => (unwrapStringLiteral(prefix), unwrapStringLiteral(uri))
    }

  def elemChildrenPart: Parser[Vector[NodeBuilder]] =
    "children" ~> "=" ~> elemChildren

  def elemChildren: Parser[Vector[NodeBuilder]] =
    "Vector" ~> "(" ~> repsep(elemChild, ",") <~ ")" ^^ { xs => Vector(xs: _*) }

  def elemChild: Parser[NodeBuilder] = (element | processingInstruction | comment | text | entityRef)

  // ProcessingInstructionBuilder

  def processingInstruction: Parser[ProcessingInstructionBuilder] =
    "processingInstruction" ~> "(" ~> stringLiteral2 ~ "," ~ stringLiteral2 <~ ")" ^^
      {
        case target ~ "," ~ data =>
          ProcessingInstructionBuilder(unwrapStringLiteral(target), unwrapStringLiteral(data))
      }

  // CommentBuilder

  def comment: Parser[CommentBuilder] =
    "comment" ~> "(" ~> concatenatedStringLiterals <~ ")" ^^ { x => CommentBuilder(x) }

  // TextBuilder

  def text: Parser[TextBuilder] = (cdata | nonCData)

  def cdata: Parser[TextBuilder] =
    "cdata" ~> "(" ~> concatenatedStringLiterals <~ ")" ^^ { x => TextBuilder(x, true) }

  def nonCData: Parser[TextBuilder] =
    "text" ~> "(" ~> concatenatedStringLiterals <~ ")" ^^ { x => TextBuilder(x, false) }

  // EntityRefBuilder

  def entityRef: Parser[EntityRefBuilder] =
    "entityRef" ~> "(" ~> stringLiteral2 <~ ")" ^^ { x => EntityRefBuilder(unwrapStringLiteral(x)) }

  // Helpers

  private def unwrapStringLiteral(literal: String): String = {
    require(literal.startsWith("\"") && literal.endsWith("\""),
      s"Expected string literal, enclosed by a pair of double quotes, but found '${literal}'")

    val content = literal.drop(1).dropRight(1)
    StringEscapeUtils.unescapeJava(content)
  }
}