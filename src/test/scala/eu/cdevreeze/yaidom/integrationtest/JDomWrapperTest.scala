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
package integrationtest

import java.{ util => jutil, io => jio }
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import org.w3c.dom
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import scala.collection.JavaConverters._
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import JDomWrapperTest._

/**
 * JDOM wrapper test case. It shows that we can easily create `ElemLike` wrappers around JDOM Elements.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class JDomWrapperTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("books.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      val elms = root.findAllElems
      (elms map (e => e.localName)).toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  @Test def testParseStrangeXml() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("strangeXml.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseDefaultNamespaceXml() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXml.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.commentChildren.map(_.text.trim) }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[DomInteropTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[DomInteropTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val is = classOf[JDomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = nsXmlSchema

    val xsElmENames: Set[EName] =
      Set(EName(ns, "schema"), EName(ns, "annotation"), EName(ns, "documentation"),
        EName(ns, "import"), EName(ns, "complexType"), EName(ns, "complexContent"),
        EName(ns, "extension"), EName(ns, "sequence"), EName(ns, "element"),
        EName(ns, "attribute"), EName(ns, "choice"), EName(ns, "group"),
        EName(ns, "simpleType"), EName(ns, "restriction"), EName(ns, "enumeration"),
        EName(ns, "list"), EName(ns, "union"), EName(ns, "key"),
        EName(ns, "selector"), EName(ns, "field"), EName(ns, "attributeGroup"),
        EName(ns, "anyAttribute"), EName(ns, "whiteSpace"), EName(ns, "fractionDigits"),
        EName(ns, "pattern"), EName(ns, "any"), EName(ns, "appinfo"),
        EName(ns, "minLength"), EName(ns, "maxInclusive"), EName(ns, "minInclusive"),
        EName(ns, "notation"))

    expect(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: JDomElem): Unit = {
      val forChoiceDefOption: Option[JDomElem] = {
        val result = rootElm filterChildElems { e => e.resolvedName == EName(ns, "simpleType") && e.attribute(EName("name")) == "formChoice" }
        result.headOption
      }

      expect(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElems(EName(ns, "documentation")) flatMap { e => e.trimmedText } mkString ""

      expect("A utility type, not for public use") {
        forChoiceDefDocumentation.trim
      }
    }

    checkForChoiceDocumentation(root)

    def checkCommentWithEscapedChar(rootElm: JDomElem): Unit = {
      val documentationElms =
        for {
          annotationElm <- rootElm \ EName(ns, "annotation")
          documentationElm <- annotationElm \ EName(ns, "documentation")
        } yield documentationElm

      val documentationText = documentationElms.drop(1).headOption map { e => e.trimmedText } getOrElse ""

      // The XML string contains "&lt;", but the parsed text should contain an unescaped "<" instead
      expect(true) {
        documentationText.containsSlice("""XML Schema language.  The documentation (within <documentation> elements)""")
      }
    }

    checkCommentWithEscapedChar(root)

    def checkIdentityConstraintElm(rootElm: JDomElem): Unit = {
      val identityConstraintElms =
        for {
          schemaElm <- rootElm filterElems { e =>
            e.resolvedName == EName(ns, "element") &&
              e.attributeOption(EName("name")) == Some("schema") &&
              e.attributeOption(EName("id")) == Some("schema")
          }
          idConstraintElm <- schemaElm filterChildElems { e =>
            e.resolvedName == EName(ns, "key") &&
              e.attributeOption(EName("name")) == Some("identityConstraint")
          }
        } yield idConstraintElm

      expect(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head \ EName(ns, "selector")

      expect(1) {
        selectorElms.size
      }

      expect(""".//xs:key|.//xs:unique|.//xs:keyref""") {
        selectorElms.head.attributeOption(EName("xpath")).getOrElse("")
      }
    }

    checkIdentityConstraintElm(root)

    def checkComplexTypeElm(rootElm: JDomElem): Unit = {
      val complexTypeElms =
        rootElm filterElems { e =>
          e.resolvedName == EName(ns, "complexType") &&
            e.attributeOption(EName("name")) == Some("element") &&
            e.attributeOption(EName("abstract")) == Some("true")
        }

      expect(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElems(EName(ns, "extension"))
      val sequenceElms = complexTypeElms.head.filterElems(EName(ns, "sequence"))
      val choiceElms = complexTypeElms.head.filterElems(EName(ns, "choice"))
      val elementElms = complexTypeElms.head.filterElems(EName(ns, "element"))
      val groupElms = complexTypeElms.head.filterElems(EName(ns, "group"))
      val attributeElms = complexTypeElms.head.filterElems(EName(ns, "attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElems(EName(ns, "attributeGroup"))

      expect(Set(EName("base"))) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }
      expect(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.values }
        result.toSet
      }

      expect(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("ref"))) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: JDomElem): Unit = {
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == EName(ns, "element") &&
          e.attributeOption(EName("name")) == Some("field") &&
          e.attributeOption(EName("id")) == Some("field")
      }

      val patternElms = fieldElms flatMap { e => e.filterElems(EName(ns, "pattern")) }

      expect(1) {
        patternElms.size
      }

      expect("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption(EName("value")).getOrElse("")
      }
    }

    checkFieldPattern(root)
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: JDomElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expect(true) {
        childOption.isDefined
      }
      expect(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      expect(text) {
        val txt = childOption.get.trimmedText
        txt.take(text.length)
      }
    }

    checkChildText(root)
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    dbf.setExpandEntityReferences(false)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: JDomElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expect(true) {
        childOption.isDefined
      }
      expect(2) {
        childOption.get.textChildren.size
      }
      expect(1) {
        val result = childOption.get.children collect { case er: JDomEntityRef => er }
        result.size
      }
      expect("hello") {
        val entityRefs = childOption.get.children collect { case er: JDomEntityRef => er }
        val entityRef: JDomEntityRef = entityRefs.head
        entityRef.wrappedNode.getName
      }
      val s = "This text contains an entity reference, viz."
      expect(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    dbf.setCoalescing(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: JDomElem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expect(2) {
        childElms.size
      }

      val text = "Jansen & co"

      // Remember: we set the parser to coalescing!
      expect(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }

      expect(Set(text)) {
        val result = childElms map { e => e.attributeOption(EName("about")).getOrElse("Missing text") }
        result.toSet
      }

      expect(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)
  }

  @Test def testParseXmlWithSpecialChars() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: JDomElem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expect(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      expect(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)
  }

  @Test def testParseGeneratedHtml() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("books.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    require(root.localName == "Bookstore")

    val htmlFormatString =
      """|<html>
         |  <body>
         |    <h1>Bookstore</h1>
         |    <table>
         |      <tr>
         |        <th>Title</th>
         |        <th>ISBN</th>
         |        <th>Edition</th>
         |        <th>Authors</th>
         |        <th>Price</th>
         |      </tr>
         |%s
         |    </table>
         |  </body>
         |</html>""".stripMargin

    val bookFormatString =
      """|      <tr>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |      </tr>""".stripMargin

    def bookHtmlString(bookElm: JDomElem): String = {
      val authorNames: immutable.IndexedSeq[String] =
        bookElm.filterElems(EName("{http://bookstore}Author")) map { e =>
          "%s %s".format(
            e.getChildElem(EName("{http://bookstore}First_Name")).trimmedText,
            e.getChildElem(EName("{http://bookstore}Last_Name")).trimmedText)
        }

      val authors = authorNames.mkString(", ")

      val result = bookFormatString.format(
        bookElm.getChildElem(EName("{http://bookstore}Title")).trimmedText,
        bookElm.attributeOption(EName("ISBN")).getOrElse(""),
        bookElm.attributeOption(EName("Edition")).getOrElse(""),
        authors,
        bookElm.attributeOption(EName("Price")).getOrElse(""))
      result
    }

    val booksHtmlString = root.filterElems(EName("{http://bookstore}Book")) map { e => bookHtmlString(e) } mkString ("\n")
    val htmlString = htmlFormatString.format(booksHtmlString)

    val domBuilder2 = new org.jdom2.input.DOMBuilder
    val doc2 = domBuilder2.build(
      dbf.newDocumentBuilder.parse(
        new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))))
    val domDoc2: JDomDocument = JDomNode.wrapDocument(doc2)

    val htmlRoot: JDomElem = domDoc2.documentElement

    val tableRowElms = htmlRoot.filterElems(EName("tr")).drop(1)

    expect(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    expect(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(3).headOption }
    val authors = authorsElms map { e => e.trimmedText }

    expect(Set(
      "Jeffrey Ullman, Jennifer Widom",
      "Hector Garcia-Molina, Jeffrey Ullman, Jennifer Widom",
      "Jeffrey Ullman, Hector Garcia-Molina",
      "Jennifer Widom")) {
      authors.toSet
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("cars.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val doc = domBuilder.build(db.parse(is))
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    expect("records") {
      domDoc.documentElement.localName
    }

    val recordsElm = domDoc.documentElement

    expect(3) {
      (recordsElm \ "car").size
    }

    expect(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ "car")(0)

    expect("car") {
      firstRecordElm.localName
    }

    expect("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    expect("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expect(2) {
      val carElms = recordsElm \ "car"
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    expect(Set("Holden", "Peel")) {
      val carElms = recordsElm \ "car"
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    expect(Set("speed", "size", "price")) {
      val result = recordsElm collectFromElemsOrSelf { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }
  }

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      // Default behaviour
      null
    }
  }
}

object JDomWrapperTest {

  sealed trait JDomNode {

    type DomType <: org.jdom2.Content

    def wrappedNode: DomType

    final override def toString: String = wrappedNode.toString
  }

  final class JDomDocument(
    val wrappedNode: org.jdom2.Document) {

    require(wrappedNode ne null)

    def documentElement: JDomElem = JDomNode.wrapElement(wrappedNode.getRootElement)
  }

  final class JDomElem(
    override val wrappedNode: org.jdom2.Element) extends JDomNode with ElemLike[JDomElem] with HasText { self =>

    require(wrappedNode ne null)

    override type DomType = org.jdom2.Element

    final def children: immutable.IndexedSeq[JDomNode] = {
      val childList = wrappedNode.getContent.asScala.toIndexedSeq
      childList flatMap { ch => JDomNode.wrapOption(ch) }
    }

    override def allChildElems: immutable.IndexedSeq[JDomElem] = children collect { case e: JDomElem => e }

    override def resolvedName: EName = {
      val jdomNs = wrappedNode.getNamespace
      val nsOption: Option[String] = if (jdomNs == org.jdom2.Namespace.NO_NAMESPACE) None else Some(jdomNs.getURI)
      EName(nsOption, wrappedNode.getName)
    }

    override def resolvedAttributes: Map[EName, String] = {
      val jdomAttrs: immutable.IndexedSeq[org.jdom2.Attribute] = wrappedNode.getAttributes.asScala.toIndexedSeq

      val result = jdomAttrs map { attr =>
        val jdomNs = attr.getNamespace
        val nsOption: Option[String] = if (jdomNs == org.jdom2.Namespace.NO_NAMESPACE) None else Some(jdomNs.getURI)
        val ename = EName(nsOption, attr.getName)

        (ename -> attr.getValue)
      }
      result.toMap
    }

    def qname: QName = QName(wrappedNode.getQualifiedName)

    def attributes: Map[QName, String] = {
      val jdomAttrs: immutable.IndexedSeq[org.jdom2.Attribute] = wrappedNode.getAttributes.asScala.toIndexedSeq

      val result = jdomAttrs map { attr =>
        (QName(attr.getQualifiedName) -> attr.getValue)
      }
      result.toMap
    }

    def scope: Scope = {
      val m: Map[String, String] = {
        val result = wrappedNode.getNamespacesInScope.asScala.toIndexedSeq map { (ns: org.jdom2.Namespace) => (ns.getPrefix -> ns.getURI) }
        result.toMap
      }
      Scope.from(m)
    }

    /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
    def attributeScope: Scope = Scope(scope.map - "")

    def declarations: Declarations = {
      val m: Map[String, String] = {
        val result = wrappedNode.getNamespacesIntroduced.asScala.toIndexedSeq map { (ns: org.jdom2.Namespace) => (ns.getPrefix -> ns.getURI) }
        result.toMap
      }
      Declarations.from(m)
    }

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[JDomText] = children collect { case t: JDomText => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[JDomComment] = children collect { case c: JDomComment => c }

    /**
     * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }
  }

  final class JDomText(override val wrappedNode: org.jdom2.Text) extends JDomNode {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.Text

    def text: String = wrappedNode.getText

    def trimmedText: String = wrappedNode.getTextTrim

    def normalizedText: String = wrappedNode.getTextNormalize
  }

  final class JDomProcessingInstruction(override val wrappedNode: org.jdom2.ProcessingInstruction) extends JDomNode {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.ProcessingInstruction
  }

  final class JDomEntityRef(override val wrappedNode: org.jdom2.EntityRef) extends JDomNode {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.EntityRef
  }

  final class JDomComment(override val wrappedNode: org.jdom2.Comment) extends JDomNode {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.Comment

    def text: String = wrappedNode.getText
  }

  object JDomNode {

    def wrapOption(node: org.jdom2.Content): Option[JDomNode] = {
      node match {
        case e: org.jdom2.Element => Some(new JDomElem(e))
        case cdata: org.jdom2.CDATA => Some(new JDomText(cdata))
        case t: org.jdom2.Text => Some(new JDomText(t))
        case pi: org.jdom2.ProcessingInstruction => Some(new JDomProcessingInstruction(pi))
        case er: org.jdom2.EntityRef => Some(new JDomEntityRef(er))
        case c: org.jdom2.Comment => Some(new JDomComment(c))
        case _ => None
      }
    }

    def wrapDocument(doc: org.jdom2.Document): JDomDocument = new JDomDocument(doc)

    def wrapElement(elm: org.jdom2.Element): JDomElem = new JDomElem(elm)
  }
}