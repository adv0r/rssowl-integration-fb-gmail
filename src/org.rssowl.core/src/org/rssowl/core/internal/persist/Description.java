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
package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.NewsReference;

public class Description extends Persistable    {

  private String fValue;

  /* Also used as its own identifier */
  private long fNewsId;

  protected Description() {
    super();
  }

  public Description(INews news, String value) {
    Assert.isNotNull(news, "news"); //$NON-NLS-1$
    Assert.isNotNull(news.getId(), "news.getId()"); //$NON-NLS-1$
    fNewsId = news.getId();

    fValue = value;
  }

  public synchronized NewsReference getNews() {
    return new NewsReference(fNewsId);
  }

  public synchronized String getValue() {
    return fValue;
  }

  public synchronized void setDescription(String description) {
    fValue = description;
  }
}
