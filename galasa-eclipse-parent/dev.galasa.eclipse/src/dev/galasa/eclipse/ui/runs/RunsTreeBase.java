/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.runs;

import java.util.ArrayList;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.IFrameworkChangeListener;
import dev.galasa.eclipse.ui.FrameworkNotInitialised;
import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;

public class RunsTreeBase implements IUIParent, IFrameworkChangeListener {

    private FrameworkNotInitialised frameworkNotInitialised;
    private RunsParent              runsParent;

    private RunsView                view;

    protected RunsTreeBase() {
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

        if (this.runsParent != null) {
            this.runsParent.dispose();
            this.runsParent = null;
        }
    }

    @Override
    public boolean hasChildren() {
        if (frameworkNotInitialised != null || runsParent != null) {
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

        if (runsParent != null) {
            children.add(runsParent);
        }

        return children.toArray();
    }

    @Override
    public void statusChanged(boolean initialised) {
        if (initialised) {
            if (frameworkNotInitialised != null) {
                frameworkNotInitialised = null;
            }

            if (runsParent == null) {
                runsParent = new RunsParent(view);

            }
        } else {
            if (frameworkNotInitialised == null) {
                frameworkNotInitialised = new FrameworkNotInitialised();
            }

            if (runsParent != null) {
                runsParent = null;
            }
        }

        if (view != null) {
            view.refresh(this);
        }

    }

    public void setView(RunsView view) {
        this.view = view;
        if (this.runsParent != null) {
            this.runsParent.setRunsView(view);
        }
    }

}
