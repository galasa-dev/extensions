/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import java.util.ArrayList;
import java.util.List;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.IFrameworkChangeListener;
import dev.galasa.eclipse.ui.FrameworkNotInitialised;
import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

public class ResultsTreeBase implements IUIParent, IFrameworkChangeListener {

    private FrameworkNotInitialised    frameworkNotInitialised;
    private ArrayList<ResultDirectory> rases = new ArrayList<>();

    private ResultsView                view;

    protected ResultsTreeBase(ResultsView resultsView) {
        this.view = resultsView;
    }

    protected void viewCreateFinished() {
        try {
            IFramework framework = Activator.getInstance().getFramework();
            statusChanged(framework.isInitialised());
        } catch (FrameworkException e) {
            Activator.log(e);
        }

        Activator.addFrameworkChangeListener(this);
    }

    protected void dispose() {
        Activator.removeFrameworkChangeListener(this);
    }

    @Override
    public boolean hasChildren() {
        if (frameworkNotInitialised != null || !rases.isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> children = new ArrayList<>();

        if (frameworkNotInitialised != null) {
            children.add(frameworkNotInitialised);
        }

        children.addAll(rases);

        return children.toArray();
    }

    @Override
    public void statusChanged(boolean initialised) {
        if (initialised) {
            if (frameworkNotInitialised != null) {
                frameworkNotInitialised = null;
            }

            try {
                IFramework framework = Activator.getInstance().getFramework();

                int sortOrder = 1;
                List<IResultArchiveStoreDirectoryService> resultDirectories = framework.getResultArchiveStore()
                        .getDirectoryServices();
                for (IResultArchiveStoreDirectoryService dirService : resultDirectories) {
                    rases.add(new ResultDirectory(view, dirService, sortOrder));
                    sortOrder++;
                }
            } catch (FrameworkException e) {
                Activator.log(e);
            }
        } else {
            if (frameworkNotInitialised == null) {
                frameworkNotInitialised = new FrameworkNotInitialised();
            }

            rases.clear();
        }

        if (view != null) {
            view.refresh(this);

            for (ResultDirectory ras : rases) {
                ras.expand(ras);
            }
        }

    }

}
