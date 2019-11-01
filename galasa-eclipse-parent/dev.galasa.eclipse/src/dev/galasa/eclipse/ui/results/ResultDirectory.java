/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public class ResultDirectory extends BranchFolder {

    private final String                              name;
    private final IResultArchiveStoreDirectoryService dirService;

    private final BranchFolder                        primaryBranch;

    public ResultDirectory(ResultsView view, IResultArchiveStoreDirectoryService dirService, int sortOrder) {
        super(view, dirService, Icon.none, sortOrder);
        this.dirService = dirService;
        setLoading(false);

        if (this.dirService.isLocal()) {
            this.name = "Local - " + this.dirService.getName();
        } else {
            this.name = "Automation - " + this.dirService.getName();
        }

        if (this.dirService.isLocal()) {
            primaryBranch = new BranchAllRunsFolder(getView(), dirService);
            branches.add(primaryBranch);
        } else {
            primaryBranch = new BranchMyRunsFolder(getView(), dirService);
            branches.add(primaryBranch);
            branches.add(new BranchAllRunsFolder(getView(), dirService));
            branches.add(new BranchOtherRequestorsFolder(getView(), dirService));
        }
        branches.add(new BranchTestClassFolder(getView(), dirService));
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    public void expand(ResultDirectory ras) {
        this.getView().expand(this);
        if (primaryBranch != null) {
            this.getView().expand(primaryBranch);

            primaryBranch.expandPrimaryBranch();
        }
    }

}
