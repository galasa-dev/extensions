package dev.galasa.eclipse.ui.run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.IRunResult;

public class FetchStoredArtifactsJob extends Job {

	private final ArtifactsPage artifactsPage;
	private final IRunResult runResult;

	public FetchStoredArtifactsJob(ArtifactsPage artifactsPage, IRunResult runResult) {
		super("Fetch Stored Artifacts Log");

		this.artifactsPage = artifactsPage;
		this.runResult     = runResult;

		this.setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try {
			Path root = runResult.getArtifactsRoot();
			
			ArtifactFolder rootFolder = new ArtifactFolder("/");

			Files.list(root).forEach(new ConsumeDirectory(runResult, rootFolder));

			this.artifactsPage.setArtifacts(rootFolder);
		} catch (Exception e) {
			artifactsPage.setError("Fetch of Run Stored Artifacts failed - " + e.getMessage());

			return new Status(Status.ERROR, Activator.PLUGIN_ID,
					"Failed", e);
		}

		return new Status(Status.OK, Activator.PLUGIN_ID, "Run Stored Artifacts Fetched");
	}

	private static class ConsumeDirectory implements Consumer<Path> {
		
		private final ArtifactFolder folder;
		private final IRunResult     runResult;

		public ConsumeDirectory(IRunResult runResult, ArtifactFolder folder) {
			this.folder    = folder;
			this.runResult = runResult;
		}

		@Override
		public void accept(Path path) {
			try {
				if (Files.isDirectory(path)) {
					ArtifactFolder newFolder = new ArtifactFolder(path.getFileName().toString());
					folder.addArtifact(newFolder);
					System.out.println("dir=" + path.toString());
					Files.list(path).forEach(new ConsumeDirectory(runResult, newFolder));
				} else {
					folder.addArtifact(new ArtifactFile(runResult, path));
					System.out.println("file=" + path.toString());
				}
			} catch(Exception e) {
				Activator.log(e);
			}
		}
	}
}
