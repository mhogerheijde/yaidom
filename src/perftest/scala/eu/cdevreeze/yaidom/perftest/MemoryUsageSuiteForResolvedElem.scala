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

package eu.cdevreeze.yaidom
package perftest

import java.io._
import java.net.URI
import scala.util.Try
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.parse._
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Node

/**
 * Concrete AbstractMemoryUsageSuite sub-class using "resolved" yaidom Elems.
 *
 * See the documentation of the super-class for the advice to run this suite in isolation only!
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class MemoryUsageSuiteForResolvedElem extends AbstractMemoryUsageSuite {

  type E = resolved.Elem

  protected def parseXmlFiles(files: Vector[File]): Vector[Try[resolved.Elem]] = {
    val docParser = getDocumentParser
    files map { f => Try(docParser.parse(f)).map(_.documentElement).map(e => resolved.Elem(e)) }
  }

  protected def createCommonRootParent(rootElems: Vector[resolved.Elem]): resolved.Elem = {
    resolved.Elem(EName("root"), Map(), rootElems)
  }

  protected def maxMemoryToFileLengthRatio: Int = 7
}
