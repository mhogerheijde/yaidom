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

package eu.cdevreeze.yaidom.queryapitests.indexed

import java.{ util => jutil }

import org.junit.runner.RunWith
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapitests.AbstractScopedElemLikeQueryTest
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.indexed.Elem
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopedElemLikeQueryTest extends AbstractScopedElemLikeQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.indexed")

  final type E = Elem

  protected final val xsdSchemaElem: Elem = {
    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val is = classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/XMLSchema.xsd")

    val domDoc = createDocumentBuilder(dbf).parse(is)

    Elem(Some(new java.net.URI("")), convert.DomConversions.convertToElem(domDoc.getDocumentElement(), Scope.Empty))
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(elem.underlyingElem)
}
