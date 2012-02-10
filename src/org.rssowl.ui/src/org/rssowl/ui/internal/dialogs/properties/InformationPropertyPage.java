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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.List;
import java.util.Set;

/**
 * Information about selected Entities.
 *
 * @author bpasero
 */
public class InformationPropertyPage implements IEntityPropertyPage {
  private List<IEntity> fEntities;
  private final DateFormat fDateFormat = OwlUI.getShortDateFormat();
  private boolean fInfoComputed = false;
  private Composite fContainer;
  private Label fDescriptionLabel;
  private Link fHomepageLink;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite, java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(!entities.isEmpty());
    fEntities = entities;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    fContainer = new Composite(parent, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Status */
    createLabel(fContainer, Messages.InformationPropertyPage_STATUS, true);

    final IBookMark bm = (IBookMark) fEntities.get(0);
    String message;

    /* Error Loading */
    if (bm.isErrorLoading()) {
      message = (String) bm.getProperty(Controller.LOAD_ERROR_KEY);
      if (!StringUtils.isSet(message))
        message = Messages.InformationPropertyPage_LOAD_FAILED_UNKNOWN;
      else
        message = NLS.bind(Messages.InformationPropertyPage_LOAD_FAILED_REASON, message);
    }

    /* Never Loaded */
    else if (bm.getMostRecentNewsDate() == null)
      message = Messages.InformationPropertyPage_NOT_LOADED;

    /* Successfully Loaded */
    else
      message = Messages.InformationPropertyPage_LOADED_OK;

    Label msgLabel = new Label(fContainer, SWT.WRAP);
    msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    ((GridData) msgLabel.getLayoutData()).widthHint = 300;
    msgLabel.setText(message);

    /* Link to Feedvalidator in case of an error */
    if (bm.isErrorLoading()) {
      new Label(fContainer, SWT.None);

      Link validateLink = new Link(fContainer, SWT.None);
      validateLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      validateLink.setText(Messages.InformationPropertyPage_FIND_OUT_MORE);
      validateLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          try {
            URI uri = new URI("http://www.feedvalidator.org/check.cgi?url=" + URIUtils.urlEncode(bm.getFeedLinkReference().getLinkAsText())); //$NON-NLS-1$

            OpenInBrowserAction action = new OpenInBrowserAction();
            action.selectionChanged(null, new StructuredSelection(uri));
            action.run();
          } catch (URISyntaxException ex) {
            Activator.safeLogError(ex.getMessage(), ex);
          }
        }
      });
    }

    /* Feed: Description */
    createLabel(fContainer, Messages.InformationPropertyPage_DESCRIPTION, true);

    fDescriptionLabel = new Label(fContainer, SWT.WRAP);
    fDescriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    ((GridData) fDescriptionLabel.getLayoutData()).widthHint = 300;

    /* Feed: Homepage */
    createLabel(fContainer, Messages.InformationPropertyPage_HOMEPAGE, true);

    fHomepageLink = new Link(fContainer, SWT.NONE);
    fHomepageLink.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Created */
    if (bm.getCreationDate() != null) {
      createLabel(fContainer, Messages.InformationPropertyPage_CREATED, true);
      createLabel(fContainer, fDateFormat.format(bm.getCreationDate()), false);
    }

    /* Last Visited */
    createLabel(fContainer, Messages.InformationPropertyPage_LAST_VISITED, true);
    if (bm.getLastVisitDate() != null)
      createLabel(fContainer, fDateFormat.format(bm.getLastVisitDate()), false);
    else
      createLabel(fContainer, Messages.InformationPropertyPage_NEVER, false);

    /* News Count */
    createLabel(fContainer, Messages.InformationPropertyPage_NEWS_COUNT, true);

    return fContainer;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#getImage()
   */
  public ImageDescriptor getImage() {
    if (!fEntities.isEmpty() && ((IBookMark) fEntities.get(0)).isErrorLoading())
      return OwlUI.getImageDescriptor("icons/ovr16/error.gif"); //$NON-NLS-1$

    return null;
  }

  private void createLabel(Composite parent, String text, boolean bold) {
    Label label = new Label(parent, SWT.None);
    label.setText(text);
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
    if (bold)
      label.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {}

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {
    return true;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {
    if (!fInfoComputed) {
      fInfoComputed = true;

      /* Load from Background */
      JobRunner.runUIUpdater(new UIBackgroundJob(fContainer) {
        private String description;
        private URI homepage;
        private int totalCount;
        private int newCount;
        private int unreadCount;
        private int updatedCount;

        @Override
        protected void runInBackground(IProgressMonitor monitor) {
          IBookMark bm = (IBookMark) fEntities.get(0);
          IFeed feed = bm.getFeedLinkReference().resolve();
          if (feed != null) {
            description = StringUtils.stripTags(feed.getDescription(), true);
            homepage = feed.getHomepage();
          }

          /* News Counts */
          List<INews> news = bm.getNews(INews.State.getVisible());
          totalCount = news.size();
          for (INews newsitem : news) {
            switch (newsitem.getState()) {
              case NEW:
                newCount++;
                break;
              case UNREAD:
                unreadCount++;
                break;
              case UPDATED:
                updatedCount++;
                break;
            }
          }
        }

        @Override
        protected void runInUI(IProgressMonitor monitor) {

          /* Description */
          fDescriptionLabel.setText(StringUtils.isSet(description) ? description : Messages.InformationPropertyPage_NONE);

          /* Homepage */
          fHomepageLink.setText(homepage != null ? "<a>" + homepage.toString() + "</a>" : Messages.InformationPropertyPage_NONE); //$NON-NLS-1$ //$NON-NLS-2$
          if (homepage != null) {
            fHomepageLink.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                OpenInBrowserAction action = new OpenInBrowserAction();
                action.selectionChanged(null, new StructuredSelection(homepage));
                action.run();
              }
            });
          }

          /* News Count */
          if (newCount != 0 && unreadCount != 0 && updatedCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_NEW_UNREAD_UPDATED, new Object[] { totalCount, newCount, unreadCount, updatedCount }), false);
          else if (newCount != 0 && unreadCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_NEW_UNREAD, new Object[] { totalCount, newCount, unreadCount }), false);
          else if (unreadCount != 0 && updatedCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_UNREAD_UPDATED, new Object[] { totalCount, unreadCount, updatedCount }), false);
          else if (newCount != 0 && updatedCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_NEW_UPDATED, new Object[] { totalCount, newCount, updatedCount }), false);
          else if (newCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_NEW, new Object[] { totalCount, newCount }), false);
          else if (unreadCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_UNREAD, new Object[] { totalCount, unreadCount }), false);
          else if (updatedCount != 0)
            createLabel(fContainer, NLS.bind(Messages.InformationPropertyPage_N_UPDATED, new Object[] { totalCount, updatedCount }), false);
          else
            createLabel(fContainer, String.valueOf(totalCount), false);

          /* Update Layout */
          fContainer.layout(true, true);
        }
      });
    }
  }
}