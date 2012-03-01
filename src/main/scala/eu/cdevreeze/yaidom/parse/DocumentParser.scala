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
package parse

import java.io.InputStream

/**
 * [[eu.cdevreeze.yaidom.Document]] parser.
 *
 * Implementing classes deal with the details of parsing XML strings/streams into yaidom `Document`s.
 * The [[eu.cdevreeze.yaidom]] package itself is agnostic of those details.
 *
 * Typical implementations use DOM, StAX or SAX, but make them easier to use in the tradition of the "template" classes
 * of the Spring framework. That is, resource management is done as much as possible by the DocumentParser,
 * typical usage is easy, and complex scenarios are still possible.
 *
 * Method `parse` takes an `InputStream` instead of `Source` object, because that works better with a DOM implementation.
 */
trait DocumentParser {

  /** Parses the input stream into a [[eu.cdevreeze.yaidom.Document]]. This method should close the input stream afterwards. */
  def parse(inputStream: InputStream): Document
}
