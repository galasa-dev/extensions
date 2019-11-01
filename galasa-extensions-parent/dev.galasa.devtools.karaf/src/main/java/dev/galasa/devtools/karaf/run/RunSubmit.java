/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.run;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IFrameworkRuns;
import dev.galasa.framework.spi.IRun;

@Command(scope = "run", name = "submit", description = "Submit a test run")
@Service
public class RunSubmit implements Action {

	@Argument(index = 0, name = "bundle", description = "The bundle the test is in", required = true)
	private String    bundle;

	@Argument(index = 1, name = "test", description = "The test name", required = true)
	private String    test;

	@Option(name = "-t", aliases = {
	"--type" }, description = "The request type", required = false, multiValued = false)
	private String    type;

	@Option(name = "-s", aliases = {
	"--stream" }, description = "The test stream the test is to be loaded from", required = false, multiValued = false)
	private String    stream;

	@Option(name = "-o", aliases = {
	"--obr" }, description = "The test OBR", required = false, multiValued = false)
	private String    obr;

	@Option(name = "-r", aliases = {
	"--repo" }, description = "The test maven repository", required = false, multiValued = false)
	private String    repo;

	@Option(name = "-i", aliases = {
	"--requestor" }, description = "The requestor id to submit with", required = false, multiValued = false)
	private String    requestor;

	@Option(name = "-g", aliases = {
	"--group" }, description = "The group name to submit with", required = false, multiValued = false)
	private String    groupName;

	@Option(name = "-c", aliases = {
	"--count" }, description = "Submit multi test runs", required = false, multiValued = false)
	private int    count;

	@Option(name = "--trace", description = "Run the test with tracing", required = false, multiValued = false)
	private String    trace;

	@Override
	public Object execute() throws Exception {

		if (this.count <= 0) {
			this.count = 1;
		}

		if (requestor == null || requestor.trim().isEmpty()) {
			requestor = System.getProperty("user.name");
			if (requestor == null || requestor.trim().isEmpty()) {
				requestor = "unknown";
			}
		}
		
		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}

		IFrameworkRuns runs = devEnv.getFramework().getFrameworkRuns();
		
		
		for(int i = 0; i < this.count; i++) {
			IRun run = runs.submitRun(type, 
					requestor, 
					bundle, 
					test, 
					repo, 
					obr, 
					stream,
					groupName, 
					false,
					Boolean.parseBoolean(trace),
					null);
			System.out.println("Run " + run.getName() + " submitted");
			devEnv.setRunName(run.getName());
		}

		return null;
	}
}
