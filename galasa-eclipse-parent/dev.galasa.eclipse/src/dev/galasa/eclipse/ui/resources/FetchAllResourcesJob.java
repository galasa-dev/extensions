/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.IPropertyListener;
import dev.galasa.eclipse.ui.PropertyUpdate;
import dev.galasa.eclipse.ui.PropertyUpdate.Type;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;

public class FetchAllResourcesJob extends Job {

    private final IPropertyListener listener;

    public FetchAllResourcesJob(IPropertyListener listener) {
        super("Fetch all resources");

        this.listener = listener;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            IFramework framework = Activator.getInstance().getFramework();
            if (!framework.isInitialised()) {
                return new Status(Status.OK, Activator.PLUGIN_ID, "Resources not fetched - Framework not intialised");
            }

            IDynamicStatusStoreService dss = framework.getDynamicStatusStoreService("framework");

            Map<String, String> runProperties = dss.getPrefix("resource.");

            for (Entry<String, String> runProperty : runProperties.entrySet()) {
                listener.propertyUpdate(new PropertyUpdate(runProperty.getKey(), runProperty.getValue(), Type.UPDATE));
            }

            listener.propertyUpdateComplete();
        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Resources fetched");
    }

}
