/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.framework.management;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.Framework;

public class ShutdownFrameworkJob extends Job {

    public ShutdownFrameworkJob() {
        super("Shutdown Galasa Framework");

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        monitor.beginTask("Shutting Down", 1);
        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);

        try {
            Framework framework = (Framework) Activator.getInstance().getFramework();
            framework.shutdown(Activator.getInstance().getConsole());
            Activator.frameworkChange(framework.isInitialised());
        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);

        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Galasa Framework initialised");
    }

}
