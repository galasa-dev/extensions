/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.ras;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IResultArchiveStore;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.teststructure.TestMethod;
import dev.galasa.framework.spi.teststructure.TestStructure;

@Command(scope = "ras", name = "display", description = "Display information about a run")
@Service
public class RasDisplay implements Action {

	@Argument(index = 0, name = "runName", description = "The run to display", required = false)
	private String    runName;

	private final ZoneId zoneId = ZoneId.systemDefault();

	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
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

		TestStructure structure = run.getTestStructure();

		System.out.println("Run Name  : " + structure.getRunName());
		System.out.println("Bundle    : " + structure.getBundle());
		System.out.println("Test      : " + structure.getTestName());
		System.out.println("Status    : " + structure.getStatus());
		System.out.println("Requestor : " + structure.getRequestor());
		System.out.println("Queued    : " + convertInstant(structure.getQueued()));
		System.out.println("Started   : " + convertInstant(structure.getStartTime()));
		System.out.println("Finished  : " + convertInstant(structure.getEndTime()));

		if (structure.getMethods() == null || structure.getMethods().isEmpty()) {
			System.out.println("No test methods recorded");
		} else {
			System.out.println("Test Methods:-");
			for(TestMethod method : structure.getMethods()) {
				if (method.getBefores() != null) {
					for(TestMethod before : method.getBefores()) {
						System.out.println("        " + before.getMethodName() + ",type=" + before.getType() + ",result=" + before.getResult());
					}
				}
				System.out.println("    " + method.getMethodName() + ",type=" + method.getType() + ",result=" + method.getResult());
				if (method.getAfters() != null) {
					for(TestMethod after : method.getAfters()) {
						System.out.println("        " + after.getMethodName() + ",type=" + after.getType() + ",result=" + after.getResult());
					}
				}
			}
		}

		//*** Artifact listing
		System.out.println("Stored Artifacts:-");
		StringBuilder sb = new StringBuilder();
		reportDirectory(sb, run.getArtifactsRoot(), "");
		System.out.println(sb);

		return null;
	}

	private void reportDirectory(StringBuilder sb, Path parent, String prefix) throws IOException {
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent);
		boolean slashed = false;
		for(Path child : directoryStream) {
			if (!slashed) {
				if (sb.length() > 0) {
					sb.append("/\n");
				}
				slashed = true;
			}
			sb.append(prefix + child.getFileName());
			
			if (Files.isRegularFile(child)) {
			    Map<String, Object> attrs = Files.readAttributes(child, "ras:contentType");
			    if (attrs != null) {
			        String contentType = (String)attrs.get("ras:contentType");
			        if (contentType != null) {
			            sb.append(", ");
			            sb.append(contentType);
			        }
			    }
			    sb.append("\n");
			} else if (Files.isDirectory(child)) {
				reportDirectory(sb, child, prefix + "  ");
			}
		}
	}

	private String convertInstant(Instant time) {
		if (time == null) {
			return "not specified";
		}

		return ZonedDateTime.ofInstant(time, this.zoneId).toString();
	}
}
