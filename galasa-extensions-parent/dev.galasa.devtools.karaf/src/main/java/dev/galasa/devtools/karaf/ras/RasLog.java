/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.ras;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IResultArchiveStore;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;

@Command(scope = "ras", name = "log", description = "Display the log")
@Service
public class RasLog implements Action {

	@Argument(index = 0, name = "runName", description = "The run to display", required = false)
	private String    runName;
	
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
		
		System.out.println(run.getLog());
		
		return null;
	}
}
