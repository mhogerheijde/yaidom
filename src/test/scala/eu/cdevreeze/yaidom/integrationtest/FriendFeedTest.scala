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
import javax.xml.parsers._
import javax.xml.transform.{ TransformerFactory, Transformer }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.Predef._
import NodeBuilder._
import parse._
import print._

/**
 * Test case using yaidom on the FriendFeed example, used in https://www.ibm.com/developerworks/library/x-scalaxml/.
 *
 * Acknowledgments: the author of the above-mentioned developerWorks article, Michael Galpin, kindly permitted the use of his FriendFeed example.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class FriendFeedTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val NsFriendFeedStats = {
    // Bogus namespace
    "http://friendfeed-stats".ns
  }

  private val statsScope = Scope.fromMap(Map("" -> NsFriendFeedStats.toString))

  @Test def testFilterFeedProcessing() {
    // Note the functional expression-oriented programming style, with many small expressions assigned to val variables.
    // Also note the abundant use of suffix "Elm" in variable names for Elems and ElemBuilders.

    // To understand the (core yaidom) ElemLike API, note that most methods deal with one of 3 core element sets:
    // child elements ("childElems"), descendant elements ("elems"), or descendant-or-self elements ("elemsOrSelf").
    // Once you realize that, you understand most of the ElemLike API, and therefore of the Elem API (and resolved.Elem API).

    // Also note that the ElemLike API does not offer a concise XPath-like experience. So there is no navigation across XPath axes, no unification
    // of elements and collections of elements, etc. Yet the small loss of conciseness is compensated by clear semantics and absence
    // of magic. It is very easy to understand what the methods in the ElemLike API do.

    // Moreover, note that yaidom is very explicit about the distinction between qualified names and expanded names (which starts to
    // matter only when using namespaces).

    // Finally, note that the code creating Elems seems a bit distant from the XML representation as XML strings. That's with good reason.
    // It is quite telling that equality is so hard to define for XML, and that XML parsers can be configured in so many ways.
    // Yaidom requires the user to be very explicit about parsing XML strings into Elems, and about printing Elems as XML strings.
    // Again, yaidom rather sacrifices just a little conciseness for more (semantic) clarity and absence of magic.

    // 1. Parse a sample feed into a Document

    // We can configure the DocumentParser (it's wrapped JAXP, after all), but we do not need that here

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[FriendFeedTest].getResourceAsStream("friend-feed.xml")
      docParser.parse(is)
    }
    val feedElm = doc.documentElement

    // 2. Check document structure

    expect("feed") {
      feedElm.localName
    }
    expect(Set("entry")) {
      val childNames = feedElm.allChildElems map { _.localName }
      childNames.toSet
    }

    // 3. Create service summaries, and check them against expected XML

    // For equality comparisons, we convert the Elems to resolved.Elems, containing no prefixes (so no qualified names), among other things.

    val twitterSummaryElm: Elem = createServiceSummary(feedElm, "twitter")

    val expectedTwitterSummaryElm: resolved.Elem =
      resolved.Elem(
        "Service".ename,
        Map("id".ename -> "twitter"),
        immutable.IndexedSeq(
          resolved.Elem(
            "UserList".ename,
            Map(),
            immutable.IndexedSeq(
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("karlerikson"))),
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("asfaq"))),
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("chrisjlee")))))))

    expect(expectedTwitterSummaryElm) {
      // There is no inter-element whitespace in this case, but removing it is a good habit before making equality comparisons
      resolved.Elem(twitterSummaryElm).removeAllInterElementWhitespace
    }

    val googleReaderSummaryElm: Elem = createServiceSummary(feedElm, "googlereader")

    val expectedGoogleReaderSummaryElm: resolved.Elem =
      resolved.Elem(
        "Service".ename,
        Map("id".ename -> "googlereader"),
        immutable.IndexedSeq(
          resolved.Elem(
            "UserList".ename,
            Map(),
            immutable.IndexedSeq(
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("misterjt")))))))

    expect(expectedGoogleReaderSummaryElm) {
      // There is no inter-element whitespace in this case, but removing it is a good habit before making equality comparisons
      resolved.Elem(googleReaderSummaryElm).removeAllInterElementWhitespace
    }

    val docPrinter: print.DocumentPrinter = {
      // Normal JAXP configuration, for pretty-printing

      val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance

      val tf: TransformerFactory = TransformerFactory.newInstance
      tf.setAttribute("indent-number", int2Integer(2))

      DocumentPrinterUsingDom.newInstance(dbf, tf)
    }

    val twitterXml: String = docPrinter.print(twitterSummaryElm)

    logger.info("Twitter summary:%n%s".format(twitterXml))

    val googleReaderXml: String = docPrinter.print(googleReaderSummaryElm)

    logger.info("Google reader summary:%n%s".format(googleReaderXml))

    // 4. Create statistics, and check it against expected XML

    // Now using namespaces in the created XML

    val statsElm: Elem = createStatistics(feedElm, List("twitter", "googlereader"))

    val expectedStatsElm: resolved.Elem =
      resolved.Elem(
        NsFriendFeedStats.ename("Stats"),
        Map(),
        immutable.IndexedSeq(
          resolved.Elem(NsFriendFeedStats.ename("Service"), Map("cnt".ename -> 3.toString, "id".ename -> "twitter"), immutable.IndexedSeq()),
          resolved.Elem(NsFriendFeedStats.ename("Service"), Map("cnt".ename -> 1.toString, "id".ename -> "googlereader"), immutable.IndexedSeq())))

    expect(expectedStatsElm) {
      // There is no inter-element whitespace in this case, but removing it is a good habit before making equality comparisons
      resolved.Elem(statsElm).removeAllInterElementWhitespace
    }

    val statsXml: String = docPrinter.print(statsElm)

    logger.info("Statistics:%n%s".format(statsXml))
  }

  private def filterFeedEntriesOnServiceName(feedElm: Elem, serviceName: String): immutable.IndexedSeq[Elem] = {
    require(feedElm.localName == "feed")

    val entryElms = feedElm \ { _.localName == "entry" }

    entryElms filter { entryElm =>
      // Assuming precisely 1 "service" child elem with precisely 1 "id" child elem
      // Using method getChildElem repeatedly
      val serviceIdElm = entryElm getChildElem { _.localName == "service" } getChildElem { _.localName == "id" }
      serviceIdElm.text.trim == serviceName
    }
  }

  private def getUserNickNameOfEntry(entryElm: Elem): String = {
    require(entryElm.localName == "entry")

    // Assuming precisely 1 "user" child elem with precisely 1 "nickname" child elem
    // Now using method getChildElemNamed repeatedly
    val nickNameElm = entryElm.getChildElemNamed("user".ename).getChildElemNamed("nickname".ename)
    nickNameElm.text.trim
  }

  private def createUserList(nickNames: immutable.Seq[String]): Elem = {
    // Creating ElemBuilders instead of Elems

    val userElms: immutable.Seq[ElemBuilder] =
      nickNames map { name =>
        elem(
          qname = "nickname".qname,
          children = List(text(name)))
      }

    val userListElm: ElemBuilder =
      elem(
        qname = "UserList".qname,
        children = userElms)

    // Building an Elem from the ElemBuilder
    userListElm.build()
  }

  private def createServiceSummary(feedElm: Elem, serviceName: String): Elem = {
    require(feedElm.localName == "feed")

    val nickNames = filterFeedEntriesOnServiceName(feedElm, serviceName) map { entryElm => getUserNickNameOfEntry(entryElm) }
    val userListElm = createUserList(nickNames)

    // Creating an Elem directly
    Elem(
      qname = "Service".qname,
      attributes = Map("id".qname -> serviceName),
      scope = Scope.Empty,
      children = immutable.IndexedSeq(userListElm))
  }

  private def createStatisticsForService(feedElm: Elem, serviceName: String): Elem = {
    require(feedElm.localName == "feed")

    val entryElms = filterFeedEntriesOnServiceName(feedElm, serviceName)

    val serviceElm: ElemBuilder =
      elem(
        qname = "Service".qname,
        attributes = Map("cnt".qname -> entryElms.size.toString, "id".qname -> serviceName))

    serviceElm.build(statsScope)
  }

  private def createStatistics(feedElm: Elem, serviceNames: immutable.Seq[String]): Elem = {
    require(feedElm.localName == "feed")

    val serviceStatisticsElms = serviceNames map { serviceName => createStatisticsForService(feedElm, serviceName) }

    Elem(
      qname = "Stats".qname,
      attributes = Map(),
      scope = statsScope,
      children = serviceStatisticsElms.toIndexedSeq)
  }
}