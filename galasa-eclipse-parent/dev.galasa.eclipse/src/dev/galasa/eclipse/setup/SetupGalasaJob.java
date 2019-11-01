/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.setup;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.progress.IProgressConstants;
import org.osgi.framework.Bundle;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ConsoleLog;
import dev.galasa.eclipse.preferences.PreferenceConstants;

public class SetupGalasaJob extends Job {

    public SetupGalasaJob() {
        super("Setup Galasa Framework");

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        monitor.beginTask("Setting up", 1);
        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);

        try {
            ConsoleLog console = Activator.getInstance().getConsole();

            console.info("Setting up the Galasa workspace");

            console.info("Creating the ~/.galasa files");

            Path galasaDir = Paths.get(System.getProperty("user.home"), ".galasa");
            if (Files.exists(galasaDir)) {
                console.info("The ~/.galasa directory already exists");
            } else {
                Files.createDirectories(galasaDir);
                console.info("Created the ~/.galasa directory");
            }

            createPropertyFiles(console, galasaDir, "Bootstrap Properties", "bootstrap.properties");
            createPropertyFiles(console, galasaDir, "Overrides Properties", "overrides.properties");
            createPropertyFiles(console, galasaDir, "Credentials Properties", "credentials.properties");
            createPropertyFiles(console, galasaDir, "CPS Properties", "cps.properties");
            createPropertyFiles(console, galasaDir, "DSS Properties", "dss.properties");

            IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
            String remoteMavenUri = preferenceStore.getString(PreferenceConstants.P_REMOTEMAVEN_URI);
            if ("https://repo.maven.apache.org/maven2/".equals(remoteMavenUri)) {
                console.info(
                        "Not creating a ~/.m2/settings.xml as the Galasa remote maven uri preference is the default location of https://repo.maven.apache.org/maven2/");
            } else {
                Path m2Dir = Paths.get(System.getProperty("user.home"), ".m2");
                if (Files.exists(m2Dir)) {
                    console.info("The ~/.m2 directory already exists");
                } else {
                    Files.createDirectories(m2Dir);
                    console.info("Created the ~/.m2 directory");
                }

                Path settings = m2Dir.resolve("settings.xml");
                if (Files.exists(settings)) {
                    console.info("The ~/.m2/settings.xml already exists, not modifying");
                } else {
                    Bundle bundle = Activator.getInstance().getBundle();
                    IPath path = new org.eclipse.core.runtime.Path("example-settings.xml");
                    URL bootUrl = FileLocator.find(bundle, path, null);
                    if (bootUrl == null) {
                        throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID,
                                "The example-settings.xml is missing from the plugin"));
                    }
                    bootUrl = FileLocator.toFileURL(bootUrl);
                    Path examplePath = Paths.get(bootUrl.toURI());

                    String exampleContents = new String(Files.readAllBytes(examplePath), "utf-8");
                    exampleContents = exampleContents.replaceAll("\\Q%%REPO%%\\E", remoteMavenUri);

                    Files.write(settings, exampleContents.getBytes());

                    console.info("Created the ~/.m2/.settings.xml example file");
                }

            }

            console.info("Setup complete");

        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);

        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Galasa Workspace setup");
    }

    private void createPropertyFiles(ConsoleLog console, Path galasaDir, String description, String filename)
            throws IOException {
        Path file = galasaDir.resolve(filename);
        if (Files.exists(file)) {
            console.info("The " + description + " file ~/.galasa/" + filename + " already exists");
        } else {
            Files.createFile(file);
            console.info("Created an empty " + description + " file ~/.galasa/" + filename);
        }
    }

}
