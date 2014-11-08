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

package eu.cdevreeze.yaidom.queryapitests

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.IsNavigableApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike
import AbstractSubtypeAwareElemLikeQueryTest._

/**
 * AbstractSubtypeAwareElemLike-based query test case, showing how to support custom XML dialects offering the
 * SubtypeAwareElemApi API, without depending on just one backing element implementation.
 *
 * This test case shows how yaidom can be used to support specific XML dialects in a reasonably type-safe manner,
 * while allowing for multiple backing element implementations.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractSubtypeAwareElemLikeQueryTest extends Suite {

  type E <: BridgeElem

  @Test def testQueryTable(): Unit = {
    val documentContent = new DocumentContent(wrappedDocumentContent)

    val tables = documentContent.findAllElemsOfType(classTag[Table])

    val expectedAncestryOrSelfENames =
      List(
        EName(OfficeNs, "document-content"),
        EName(OfficeNs, "body"),
        EName(OfficeNs, "spreadsheet"),
        EName(TableNs, "table"))

    assertResult(Set(expectedAncestryOrSelfENames)) {
      tables.map(_.ancestryOrSelfENames).toSet
    }

    val firstTable = tables.head

    assertResult(Set("co1", "co2")) {
      firstTable.columns.map(_.styleName).toSet
    }
    assertResult(Set("ro1")) {
      firstTable.rows.map(_.styleName).toSet
    }

    assertResult(true) {
      firstTable.rows.flatMap(_.cells).exists(_.cellText == "JavaServer Pages")
    }

    assertResult(documentContent.findAllElemsOrSelf.map(_.resolvedName)) {
      eu.cdevreeze.yaidom.resolved.Elem(documentContent.bridgeElem.toElem).findAllElemsOrSelf.map(_.resolvedName)
    }
  }

  @Test def testEqualities(): Unit = {
    val documentContent = new DocumentContent(wrappedDocumentContent)

    assertResult(documentContent.findAllElemsOfType(classTag[TableCell])) {
      documentContent.findAllElemsOfType(classTag[Table]).flatMap(_.rows).flatMap(_.cells)
    }

    assertResult(documentContent.findAllElemsOfType(classTag[Table]).flatMap(_.rows).flatMap(_.cells).flatMap(_.findAllElemsOfType(classTag[Paragraph]))) {
      documentContent.filterElemsOrSelfOfType(classTag[Paragraph]) { e =>
        val ancestryOrSelfENames = e.ancestryOrSelfENames
        Set("table", "table-row", "table-cell", "p").subsetOf(ancestryOrSelfENames.map(_.localPart).toSet)
      }
    }
  }

  protected val wrappedDocumentContent: E
}

object AbstractSubtypeAwareElemLikeQueryTest {

  val OfficeNs = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  val StyleNs = "urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  val TableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
  val TextNs = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"

  /**
   * Bridge between SpreadsheetElem API and native element implementation. It is easy to use, having no type
   * parameters with intimidating type constraints, and without paying any "cake pattern tax".
   *
   * This bridge element must know at least the names of the ancestor elements. Moreover, it must know about
   * qualified and expanded (element and attribute) names, in-scope namespaces, child elements etc.
   */
  abstract class BridgeElem {

    type BackingElem

    type SelfType <: BridgeElem

    def backingElem: BackingElem

    def findAllChildElems: immutable.IndexedSeq[SelfType]

    def resolvedName: EName

    def resolvedAttributes: immutable.Iterable[(EName, String)]

    def qname: QName

    def attributes: immutable.Iterable[(QName, String)]

    def scope: Scope

    def text: String

    def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType]

    def ancestryOrSelfENames: immutable.IndexedSeq[EName]

    def toElem: eu.cdevreeze.yaidom.simple.Elem
  }

  /**
   * Super-class of elements in an ODS spreadsheet content.xml file. It offers the `SubtypeAwareElemApi` API, among
   * other query API traits.
   */
  sealed class SpreadsheetElem(val bridgeElem: BridgeElem) extends ScopedElemLike[SpreadsheetElem] with IsNavigable[SpreadsheetElem] with SubtypeAwareElemLike[SpreadsheetElem] {

    final def findAllChildElems: immutable.IndexedSeq[SpreadsheetElem] =
      bridgeElem.findAllChildElems.map(e => SpreadsheetElem(e))

    final def resolvedName: EName = bridgeElem.resolvedName

    final def resolvedAttributes: immutable.Iterable[(EName, String)] = bridgeElem.resolvedAttributes

    final def qname: QName = bridgeElem.qname

    final def attributes: immutable.Iterable[(QName, String)] = bridgeElem.attributes

    final def scope: Scope = bridgeElem.scope

    final def text: String = bridgeElem.text

    final def findChildElemByPathEntry(entry: Path.Entry): Option[SpreadsheetElem] =
      bridgeElem.findChildElemByPathEntry(entry).map(e => SpreadsheetElem(e))

    final def ancestryOrSelfENames: immutable.IndexedSeq[EName] = bridgeElem.ancestryOrSelfENames

    override def equals(other: Any): Boolean = other match {
      case e: SpreadsheetElem => bridgeElem == e.bridgeElem
      case _ => false
    }

    override def hashCode: Int = bridgeElem.hashCode
  }

  final class DocumentContent(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) {
    require(resolvedName == EName(Some(OfficeNs), "document-content"))
  }

  final class Body(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) {
    require(resolvedName == EName(Some(OfficeNs), "body"))
  }

  final class Spreadsheet(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) {
    require(resolvedName == EName(Some(OfficeNs), "spreadsheet"))
  }

  final class Table(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table"))

    def columns: immutable.IndexedSeq[TableColumn] = findAllChildElemsOfType(classTag[TableColumn])

    def rows: immutable.IndexedSeq[TableRow] = findAllChildElemsOfType(classTag[TableRow])
  }

  final class TableRow(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-row"))

    def cells: immutable.IndexedSeq[TableCell] = findAllChildElemsOfType(classTag[TableCell])
  }

  final class TableColumn(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-column"))
  }

  final class TableCell(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-cell"))

    def cellText: String = findAllChildElemsOfType(classTag[Paragraph]).map(_.text).mkString
  }

  final class Paragraph(bridgeElem: BridgeElem) extends SpreadsheetElem(bridgeElem) with HasStyle {
    require(resolvedName == EName(Some(TextNs), "p"))
  }

  trait HasStyle extends SpreadsheetElem {

    final def styleName: String = attributeOption(EName(TableNs, "style-name")).getOrElse("")
  }

  object SpreadsheetElem {

    def apply(elem: BridgeElem): SpreadsheetElem = {
      elem.resolvedName match {
        case EName(Some(OfficeNs), "document-content") =>
          new DocumentContent(elem)
        case EName(Some(OfficeNs), "body") =>
          new Body(elem)
        case EName(Some(OfficeNs), "spreadsheet") =>
          new Spreadsheet(elem)
        case EName(Some(TableNs), "table") =>
          new Table(elem)
        case EName(Some(TableNs), "table-row") =>
          new TableRow(elem)
        case EName(Some(TableNs), "table-column") =>
          new TableColumn(elem)
        case EName(Some(TableNs), "table-cell") =>
          new TableCell(elem)
        case EName(Some(TextNs), "p") =>
          new Paragraph(elem)
        case _ =>
          new SpreadsheetElem(elem)
      }
    }
  }
}
