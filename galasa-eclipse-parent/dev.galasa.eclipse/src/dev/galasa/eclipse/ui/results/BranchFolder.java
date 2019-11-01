/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.eclipse.ui.results.BranchSelectedRuns.DateRange;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public abstract class BranchFolder extends Branch implements IUIParent {

    protected final static LoadingPleaseWait loadingPleaseWait = new LoadingPleaseWait();
    protected final ArrayList<Branch>        branches          = new ArrayList<>();

    private boolean                          loading           = true;

    private final int                        sortOrder;

    protected enum Icon {
        none
    };

    private final Icon                                icon;
    private final IResultArchiveStoreDirectoryService dirService;

    protected BranchFolder(ResultsView view, IResultArchiveStoreDirectoryService dirService, Icon icon, int sortOrder) {
        super(view);
        this.icon = icon;
        this.dirService = dirService;
        this.sortOrder = sortOrder;
    }

    @Override
    public boolean hasChildren() {
        synchronized (branches) {
            return !branches.isEmpty() || loading;
        }
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> children = new ArrayList<>();
        if (loading) {
            children.add(loadingPleaseWait);
        }
        synchronized (branches) {
            children.addAll(branches);
        }

        return children.toArray();
    }

    public abstract void dispose();

    public abstract void refresh();

    protected void addDateFolders(String requestor, String testClass, boolean includeOlder) {
        this.loading = false;
        ZonedDateTime now = ZonedDateTime.now();
        int dow = now.getDayOfWeek().getValue();

        branches.add(
                new BranchSelectedRuns(getView(), dirService, "Today's runs", requestor, testClass, DateRange.TODAY));
        branches.add(new BranchSelectedRuns(getView(), dirService, "Yesterdays's runs", requestor, testClass,
                DateRange.YESTERDAY));

        if (dow > 2) {
            branches.add(new BranchSelectedRuns(getView(), dirService, "Earlier this week's runs", requestor, testClass,
                    DateRange.EARLIER_THIS_WEEK));
        }
        branches.add(new BranchSelectedRuns(getView(), dirService, "Last week's runs", requestor, testClass,
                DateRange.LAST_WEEK));
        if (includeOlder) {
            branches.add(
                    new BranchSelectedRuns(getView(), dirService, "Older runs", requestor, testClass, DateRange.OLDER));
        }
    }

    protected void setLoading(boolean newLoading) {
        this.loading = newLoading;
    }

    public void expandPrimaryBranch() {
    }

    public int getSortOrder() {
        return this.sortOrder;
    }

}
