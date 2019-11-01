/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import org.eclipse.jface.preference.IPreferenceStore;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.preferences.PreferenceConstants;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public class BranchMyRunsFolder extends BranchFolder {

    private final String requestorId;

    protected BranchMyRunsFolder(ResultsView view, IResultArchiveStoreDirectoryService dirService) {
        super(view, dirService, Icon.none, 1);

        IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
        String requestorId = preferenceStore.getString(PreferenceConstants.P_REQUESTOR_ID);
        if (requestorId.isEmpty()) {
            requestorId = preferenceStore.getDefaultString(PreferenceConstants.P_REQUESTOR_ID);
        }
        this.requestorId = requestorId;

        addDateFolders(requestorId, null, true);

    }

    @Override
    public String toString() {
        return "My runs (" + requestorId + ")";
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
