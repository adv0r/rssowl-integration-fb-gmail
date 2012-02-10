/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.core.internal.interpreter;

import org.jdom.Attribute;
import org.jdom.Element;
import org.rssowl.core.interpreter.INamespaceHandler;
import org.rssowl.core.persist.IPersistable;

/**
 * Handler for the Media Namespace.
 * <p>
 * Namespace Prefix: media<br>
 * Namespace URI: http://search.yahoo.com/mrss/
 * </p>
 *
 * @author bpasero
 */
public class MediaNamespaceHandler implements INamespaceHandler {

  /*
   * @see
   * org.rssowl.core.interpreter.INamespaceHandler#processAttribute(org.jdom
   * .Attribute, org.rssowl.core.persist.IPersistable)
   */
  public void processAttribute(Attribute attribute, IPersistable type) {}

  /*
   * @see
   * org.rssowl.core.interpreter.INamespaceHandler#processElement(org.jdom.Element
   * , org.rssowl.core.persist.IPersistable)
   */
  public void processElement(Element element, IPersistable type) {}
}