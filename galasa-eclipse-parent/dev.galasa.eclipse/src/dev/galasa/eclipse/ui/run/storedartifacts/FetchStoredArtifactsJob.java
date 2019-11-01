/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.IRunResult;

public class FetchStoredArtifactsJob extends Job {

    private final ArtifactsPage artifactsPage;
    private final IRunResult    runResult;

    public FetchStoredArtifactsJob(ArtifactsPage artifactsPage, IRunResult runResult) {
        super("Fetch Stored Artifacts Log");

        this.artifactsPage = artifactsPage;
        this.runResult = runResult;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
            IConfigurationElement[] elements = extensionRegistry
                    .getConfigurationElementsFor("dev.galasa.eclipse.extension.storedartifacts.filter");

            Path root = runResult.getArtifactsRoot();

            ArtifactFolder rootFolder = new ArtifactFolder("/");

            Files.list(root).forEach(new ConsumeDirectory(runResult, rootFolder));

            if (elements != null) {
                for (IConfigurationElement element : elements) {
                    try {
                        IStoredArtifactsFilter filterClass = (IStoredArtifactsFilter) element
                                .createExecutableExtension("class");
                        String runId = "unknown";
                        if (runResult.getTestStructure() != null && runResult.getTestStructure().getRunName() != null) {
                            runId = runResult.getTestStructure().getRunName();
                        }
                        filterClass.filter(runId, rootFolder);
                    } catch (Exception e1) {
                        Activator.log(e1);
                    }
                }
            }

            this.artifactsPage.setArtifacts(rootFolder);
        } catch (Exception e) {
            artifactsPage.setError("Fetch of Run Stored Artifacts failed - " + e.getMessage());

            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Run Stored Artifacts Fetched");
    }

    private static class ConsumeDirectory implements Consumer<Path> {

        private final ArtifactFolder folder;
        private final IRunResult     runResult;

        public ConsumeDirectory(IRunResult runResult, ArtifactFolder folder) {
            this.folder = folder;
            this.runResult = runResult;
        }

        @Override
        public void accept(Path path) {
            try {
                if (Files.isDirectory(path)) {
                    ArtifactFolder newFolder = new ArtifactFolder(path.getFileName().toString());
                    folder.addArtifact(newFolder);
                    Files.list(path).forEach(new ConsumeDirectory(runResult, newFolder));
                } else {
                    folder.addArtifact(new ArtifactFile(runResult, path));
                }
            } catch (Exception e) {
                Activator.log(e);
            }
        }
    }
}
