/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public class BranchOtherRequestorsFolder extends BranchFolder {

    protected BranchOtherRequestorsFolder(ResultsView view, IResultArchiveStoreDirectoryService dirService) {
        super(view, dirService, Icon.none, 3);
    }

    @Override
    public String toString() {
        return "Other people's runs";
    }

    @Override
    public void dispose() {
    }

    @Override
    public void refresh() {
    }

}
