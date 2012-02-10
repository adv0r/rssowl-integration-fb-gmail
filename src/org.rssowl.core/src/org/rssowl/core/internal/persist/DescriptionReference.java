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

import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.persist.service.PersistenceException;


public final class DescriptionReference {

  private final long fNewsId;
  public DescriptionReference(long newsId) {
    fNewsId = newsId;
  }

  public long getNewsId() {
    return fNewsId;
  }

  public Description resolve() throws PersistenceException  {
    return DBHelper.getDescriptionDAO().load(fNewsId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    DescriptionReference other = (DescriptionReference) obj;
    return fNewsId == other.fNewsId;
  }

  @Override
  public int hashCode() {
    return (int) (fNewsId ^ (fNewsId >>> 32));
  }

  @Override
  public String toString() {
    String name = super.toString();
    int index = name.lastIndexOf('.');
    if (index != -1)
      name = name.substring(index + 1, name.length());

    return name + " (ID = " + fNewsId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
  }
}
