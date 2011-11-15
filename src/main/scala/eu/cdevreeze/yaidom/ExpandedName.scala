package eu.cdevreeze.yaidom

import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }

/**
 * Expanded name. See http://www.w3.org/TR/xml-names11/. It has a localPart and an optional namespace URI.
 * Semantically like a QName in Java, but not keeping the prefix.
 *
 * To get an ExpandedName from a QName, the latter needs to be resolved against a Scope.
 */
final case class ExpandedName(namespaceUri: Option[String], localPart: String) extends Immutable {
  require(namespaceUri ne null)
  require(namespaceUri.forall(ns => (ns ne null) && (ns.length > 0)))
  require(localPart ne null)
  require(localPart.length > 0)

  /** Given an optional prefix, creates a QName from this ExpandedName */
  def toQName(prefix: Option[String]): QName = {
    require(namespaceUri.isDefined || prefix.isEmpty)
    QName(prefix, localPart)
  }

  /** Given an optional prefix, creates a javax.xml.namespace.QName from this ExpandedName */
  def toJavaQName(prefix: Option[String]): JQName = {
    require(namespaceUri.isDefined || prefix.isEmpty)
    new JQName(namespaceUri.getOrElse(XMLConstants.NULL_NS_URI), localPart, prefix.getOrElse(XMLConstants.DEFAULT_NS_PREFIX))
  }

  /** The String representation, in the format of the javax.xml.namespace.QName.toString method */
  override def toString: String = toJavaQName(None).toString
}

object ExpandedName {

  /** Creates an ExpandedName from a namespaceUri and a localPart */
  def apply(namespaceUri: String, localPart: String): ExpandedName = ExpandedName(Some(namespaceUri), localPart)

  /** Creates an ExpandedName from a localPart only */
  def apply(localPart: String): ExpandedName = ExpandedName(None, localPart)

  /** Creates an ExpandedName from a javax.xml.namespace.QName */
  def fromJavaQName(jqname: JQName): ExpandedName = jqname match {
    case jqname: JQName if (jqname.getNamespaceURI eq null) || (jqname.getNamespaceURI == XMLConstants.NULL_NS_URI) =>
      ExpandedName(jqname.getLocalPart)
    case _ => ExpandedName(jqname.getNamespaceURI, jqname.getLocalPart)
  }

  /** Gets an optional prefix from a javax.xml.namespace.QName */
  def prefixFromJavaQName(jqname: JQName): Option[String] = {
    val prefix: String = jqname.getPrefix
    if ((prefix eq null) || (prefix == XMLConstants.DEFAULT_NS_PREFIX)) None else Some(prefix)
  }
}
