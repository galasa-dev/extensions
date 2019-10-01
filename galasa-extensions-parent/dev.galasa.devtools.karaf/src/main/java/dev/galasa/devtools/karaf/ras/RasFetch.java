package dev.galasa.devtools.karaf.ras;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IResultArchiveStore;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;

@Command(scope = "ras", name = "fetch", description = "Fetch an artifact")
@Service
public class RasFetch implements Action {

	@Argument(index = 0, name = "runName", description = "The run to display", required = false)
	private String    runName;

	@Argument(index = 1, name = "artifact", description = "The artifact to fetch", required = true)
	private String    artifact;

	@Option(name = "-d", aliases = {
	"--display" }, description = "display on screen", required = false, multiValued = false)
	private boolean    display;


	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}

		if (!display) {
			System.err.println("Cannot download yet,  use -d");
			return null;
		}

		if (this.runName == null) {
			this.runName = devEnv.getRunName();
		}

		if (this.runName == null) {
			System.err.println("No Run name has been provided");
			return null;
		}

		this.runName = runName.toUpperCase();

		IResultArchiveStore ras = devEnv.getFramework().getResultArchiveStore();
		List<IResultArchiveStoreDirectoryService> rasDirs = ras.getDirectoryServices();

		ArrayList<IRunResult> allRuns = new ArrayList<>();
		for(IResultArchiveStoreDirectoryService rasDir : rasDirs) {
			allRuns.addAll(rasDir.getRuns(this.runName));
		}
		if (allRuns.isEmpty()) {
			System.err.println("Unable to locate run " + this.runName);
			return null;
		}

		if (allRuns.size() > 1) {
			System.err.println("The RAS has returned multiple runs for this run name, at the moment I can't handle that, because my developer was lazy");
			return null;
		}

		IRunResult run = allRuns.get(0);

		Path artifactPath = locateArtifact(run.getArtifactsRoot(), this.artifact);
		if (artifactPath == null) {
			System.err.println("Unable to locate artifact " + this.artifact + " from run " + this.runName);
			return null;
		}

		if (display) {
			System.out.println("Artifact " + artifactPath.toString());
			byte[] data = Files.readAllBytes(artifactPath);
			System.out.println(new String(data, "utf-8"));
		}

		return null;
	}


	private Path locateArtifact(Path parent, String artifact) throws IOException {
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent);
		for(Path child : directoryStream) {
			if (child.getFileName().toString().equals(artifact)) {
				return child;
			}

			if (Files.isDirectory(child)) {
				Path childPath = locateArtifact(child, artifact);
				if (childPath != null) {
					return childPath;
				}
			}
		}		

		return null;
	}
}
