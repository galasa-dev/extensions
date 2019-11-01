/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.framework.management;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.progress.IProgressConstants;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ConsoleLog;
import dev.galasa.eclipse.preferences.PreferenceConstants;
import dev.galasa.framework.FrameworkInitialisation;

public class InitialiseFrameworkJob extends Job {

    public InitialiseFrameworkJob() {
        super("Initialise Galasa Framework");

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        monitor.beginTask("Initialising", 1);
        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);

        try {
            Properties bootstrapProperties = new Properties();
            Properties overrideProperties = new Properties();

            // *** Request that the default local ras is included
            overrideProperties.put("framework.resultarchive.store.include.default.local", "true");

            // *** Retrieve the preferences
            IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
            String bootstrapUri = preferenceStore.getString(PreferenceConstants.P_BOOTSTRAP_URI);
            String overrideUri = preferenceStore.getString(PreferenceConstants.P_OVERRIDES_URI);

            URL bootstrapURL = new URL(bootstrapUri);
            try (InputStream is = bootstrapURL.openStream()) {
                bootstrapProperties.load(is);
            }

            if (!overrideUri.isEmpty()) {
                URL overridesUrl = new URL(overrideUri);
                try (InputStream is = overridesUrl.openStream()) {
                    overrideProperties.load(is);
                }
            }

            // Add the bootstrap url to the overrides for the benefit of the managers
            overrideProperties.put("framework.bootstrap.url", bootstrapUri);

            ConsoleLog console = Activator.getInstance().getConsole();

            FrameworkInitialisation fi = new FrameworkInitialisation(bootstrapProperties, overrideProperties, false,
                    console);

            Activator.frameworkChange(fi.getFramework().isInitialised());
        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);

        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Galasa Framework initialised");
    }

}
