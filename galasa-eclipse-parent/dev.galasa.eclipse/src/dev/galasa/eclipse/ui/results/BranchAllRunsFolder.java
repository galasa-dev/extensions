/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public class BranchAllRunsFolder extends BranchFolder {

    protected BranchAllRunsFolder(ResultsView view, IResultArchiveStoreDirectoryService dirService) {
        super(view, dirService, Icon.none, 2);

        addDateFolders(null, null, dirService.isLocal());
    }

    @Override
    public String toString() {
        return "All runs";
    }

    @Override
    public void dispose() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void expandPrimaryBranch() {
        getView().expand(branches.get(0));
        branches.get(0).load();
    }
}
