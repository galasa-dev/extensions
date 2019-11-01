/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;

public class FetchStoredArtifactJob extends Job {

    private final ArtifactEditor artifactEditor;
    private final Path           path;

    public FetchStoredArtifactJob(ArtifactEditor artifactEditor, Path path) {
        super("Fetch Stored Artifact Log");

        this.artifactEditor = artifactEditor;
        this.path = path;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            String text = new String(Files.readAllBytes(path));
            this.artifactEditor.setText(text);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            artifactEditor.setText("Fetch of stored artifact failed\n" + sw.toString());

            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Stored Artifact Fetched");
    }

}
