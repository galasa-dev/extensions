/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import java.util.ArrayList;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.IFrameworkChangeListener;
import dev.galasa.eclipse.ui.FrameworkNotInitialised;
import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;

public class ResourcesTreeBase implements IUIParent, IFrameworkChangeListener {

    private FrameworkNotInitialised frameworkNotInitialised;
    private ResourcesParent         resourcesParent;

    private ResourcesView           view;

    protected ResourcesTreeBase() {
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

        if (this.resourcesParent != null) {
            this.resourcesParent.dispose();
            this.resourcesParent = null;
        }
    }

    @Override
    public boolean hasChildren() {
        if (frameworkNotInitialised != null || resourcesParent != null) {
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

        if (resourcesParent != null) {
            children.add(resourcesParent);
        }

        return children.toArray();
    }

    @Override
    public void statusChanged(boolean initialised) {
        if (initialised) {
            if (frameworkNotInitialised != null) {
                frameworkNotInitialised = null;
            }

            if (resourcesParent == null) {
                resourcesParent = new ResourcesParent(view);

            }
        } else {
            if (frameworkNotInitialised == null) {
                frameworkNotInitialised = new FrameworkNotInitialised();
            }

            if (resourcesParent != null) {
                resourcesParent = null;
            }
        }

        if (view != null) {
            view.refresh(this);
        }

    }

    public void setView(ResourcesView view) {
        this.view = view;
        if (this.resourcesParent != null) {
            this.resourcesParent.setResourcesView(view);
        }
    }

}
