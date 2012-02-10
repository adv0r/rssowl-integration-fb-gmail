/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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
package org.rssowl.ui.internal.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.rssowl.ui.internal.Activator;

public class FacebookJob extends Job {

  private int state ;

  public final int JOB_BEGIN =  0;
  public final int JOB_LOOKING_UID_PASSWORD =  100;
  public final int JOB_LOOKING_FRIENDSKEY =  102;
  public final int JOB_OVER =  -1;

  @SuppressWarnings("unused")
  private int doNothing;

  public FacebookJob(String name) {
    super(name);
    state = JOB_BEGIN;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      monitor.beginTask("Geting the source of your feeds...", 4); //$NON-NLS-1$

      monitor.subTask("Authenticating..."); //$NON-NLS-1$
      do{ doNothing=1; }  //Thread.sleep(100);
      while(state == JOB_BEGIN);
      monitor.worked(1);

      monitor.subTask("Obtaining data..."); //$NON-NLS-1$
      do{  doNothing=1;  }
      while(state == JOB_LOOKING_UID_PASSWORD);
      monitor.worked(1);

      monitor.subTask("Completing procedure..."); //$NON-NLS-1$
      do{  doNothing=1;  }
      while(state == JOB_LOOKING_FRIENDSKEY);
      monitor.worked(1);
      monitor.done();
    }

    catch (Exception e) {
      Activator.safeLogError("JobError", e);        } //$NON-NLS-1$

    finally {
      monitor.done();
    }

    return Status.OK_STATUS;
  }

  public void setState(int state) {
    this.state = state;
  }

}
