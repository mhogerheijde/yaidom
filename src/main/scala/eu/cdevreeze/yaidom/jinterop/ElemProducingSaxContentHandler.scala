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
package jinterop

import org.xml.sax.helpers.DefaultHandler
import scala.collection.immutable
import eu.cdevreeze.yaidom._

/**
 * Contract of a SAX ContentHandler that, once ready, can be asked for the resulting [[eu.cdevreeze.yaidom.Elem]] using
 * method <code>resultingElem</code>, or the resulting [[eu.cdevreeze.yaidom.Document]] using method
 * <code>resultingDocument</code>.
 */
trait ElemProducingSaxContentHandler extends DefaultHandler {

  /** Returns the resulting Elem. Do not call before SAX parsing is ready. */
  def resultingElem: Elem

  /** Returns the resulting Document. Do not call before SAX parsing is ready. */
  def resultingDocument: Document
}
