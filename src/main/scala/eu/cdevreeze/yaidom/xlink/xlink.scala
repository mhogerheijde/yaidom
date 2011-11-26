package eu.cdevreeze.yaidom
package xlink

import java.net.URI
import scala.collection.immutable
import XLink._

/** XLink or a part thereof (or even the parent, such as the root element of a linkbase) */
sealed trait XLinkPart extends ElemLike[XLinkPart] with Immutable {

  val wrappedElem: Elem

  require(wrappedElem ne null)

  final val resolvedName: ExpandedName = wrappedElem.resolvedName

  final val childElems: immutable.Seq[XLinkPart] = wrappedElem.childElems.map(e => XLinkPart(e))
}

/** XLink */
trait XLink extends XLinkPart {
  require(wrappedElem.attributeOption(XLinkTypeExpandedName).isDefined, "Missing %s".format(XLinkTypeExpandedName))

  def xlinkType: String = wrappedElem.attribute(XLinkTypeExpandedName)

  def xlinkAttributes: Map[ExpandedName, String] = wrappedElem.resolvedAttributes.filterKeys(a => a.namespaceUri == Some(XLinkNamespace.toString))

  def arcroleOption: Option[String] = wrappedElem.attributeOption(XLinkArcroleExpandedName)
}

/** Simple or extended link */
trait Link extends XLink

final case class SimpleLink(override val wrappedElem: Elem) extends Link {
  require(xlinkType == "simple")
  require(wrappedElem.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))

  def href: URI = wrappedElem.attributeOption(XLinkHrefExpandedName).map(s => URI.create(s)).getOrElse(sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateExpandedName)
}

final case class ExtendedLink(override val wrappedElem: Elem) extends Link {
  require(xlinkType == "extended")

  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)

  def titleXLinks: immutable.Seq[Title] = childElems collect { case xlink: Title => xlink }
  def locatorXLinks: immutable.Seq[Locator] = childElems collect { case xlink: Locator => xlink }
  def arcXLinks: immutable.Seq[Arc] = childElems collect { case xlink: Arc => xlink }
  def resourceXLinks: immutable.Seq[Resource] = childElems collect { case xlink: Resource => xlink }
}

final case class Arc(override val wrappedElem: Elem) extends XLink {
  require(xlinkType == "arc")
  require(arcroleOption.isDefined, "Missing %s".format(XLinkArcroleExpandedName))
  require(wrappedElem.attributeOption(XLinkFromExpandedName).isDefined, "Missing %s".format(XLinkFromExpandedName))
  require(wrappedElem.attributeOption(XLinkToExpandedName).isDefined, "Missing %s".format(XLinkToExpandedName))

  def from: String = wrappedElem.attributeOption(XLinkFromExpandedName).getOrElse(sys.error("Missing %s".format(XLinkFromExpandedName)))
  def to: String = wrappedElem.attributeOption(XLinkToExpandedName).getOrElse(sys.error("Missing %s".format(XLinkToExpandedName)))
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateExpandedName)
  def orderOption: Option[String] = wrappedElem.attributeOption(XLinkOrderExpandedName)
  def useOption: Option[String] = wrappedElem.attributeOption(XLinkUseExpandedName)
  def priorityOption: Option[String] = wrappedElem.attributeOption(XLinkPriorityExpandedName)

  def titleXLinks: immutable.Seq[Title] = childElems collect { case xlink: Title => xlink }
}

final case class Locator(override val wrappedElem: Elem) extends XLink {
  require(xlinkType == "locator")
  require(wrappedElem.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))
  require(wrappedElem.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def href: URI = wrappedElem.attributeOption(XLinkHrefExpandedName).map(s => URI.create(s)).getOrElse(sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def label: String = wrappedElem.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)

  def titleXLinks: immutable.Seq[Title] = childElems collect { case xlink: Title => xlink }
}

final case class Resource(override val wrappedElem: Elem) extends XLink {
  require(xlinkType == "resource")
  require(wrappedElem.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def label: String = wrappedElem.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
}

final case class Title(override val wrappedElem: Elem) extends XLink {
  require(xlinkType == "title")
}

object XLinkPart {

  def apply(e: Elem): XLinkPart = e match {
    case e if mustBeSimpleLink(e) => SimpleLink(e)
    case e if mustBeExtendedLink(e) => ExtendedLink(e)
    case e if mustBeTitle(e) => Title(e)
    case e if mustBeLocator(e) => Locator(e)
    case e if mustBeArc(e) => Arc(e)
    case e if mustBeResource(e) => Resource(e)
    case e if mustBeXLink(e) => {
      new {
        val wrappedElem: Elem = e
      } with XLink
    }
    case e => {
      new {
        val wrappedElem: Elem = e
      } with XLinkPart
    }
  }
}

object XLink {

  val XLinkNamespace = URI.create("http://www.w3.org/1999/xlink")

  val XLinkTypeExpandedName = ExpandedName(XLinkNamespace.toString, "type")
  val XLinkHrefExpandedName = ExpandedName(XLinkNamespace.toString, "href")
  val XLinkArcroleExpandedName = ExpandedName(XLinkNamespace.toString, "arcrole")
  val XLinkRoleExpandedName = ExpandedName(XLinkNamespace.toString, "role")
  val XLinkTitleExpandedName = ExpandedName(XLinkNamespace.toString, "title")
  val XLinkShowExpandedName = ExpandedName(XLinkNamespace.toString, "show")
  val XLinkActuateExpandedName = ExpandedName(XLinkNamespace.toString, "actuate")
  val XLinkFromExpandedName = ExpandedName(XLinkNamespace.toString, "from")
  val XLinkToExpandedName = ExpandedName(XLinkNamespace.toString, "to")
  val XLinkLabelExpandedName = ExpandedName(XLinkNamespace.toString, "label")
  val XLinkOrderExpandedName = ExpandedName(XLinkNamespace.toString, "order")
  val XLinkUseExpandedName = ExpandedName(XLinkNamespace.toString, "use")
  val XLinkPriorityExpandedName = ExpandedName(XLinkNamespace.toString, "priority")

  def mustBeXLink(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName).isDefined

  def mustBeLink(e: Elem): Boolean = mustBeSimpleLink(e) || mustBeExtendedLink(e)

  def mustBeSimpleLink(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("simple")

  def mustBeExtendedLink(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("extended")

  def mustBeTitle(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("title")

  def mustBeLocator(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("locator")

  def mustBeArc(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("arc")

  def mustBeResource(e: Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("resource")
}
