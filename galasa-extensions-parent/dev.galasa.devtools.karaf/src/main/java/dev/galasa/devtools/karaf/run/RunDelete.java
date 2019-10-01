package dev.galasa.devtools.karaf.run;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IFrameworkRuns;

@Command(scope = "run", name = "delete", description = "Delete a test run")
@Service
public class RunDelete implements Action {

	@Argument(index = 0, name = "runname", description = "The run to delete", required = true)
	private String    runname;

	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}

		IFrameworkRuns runs = devEnv.getFramework().getFrameworkRuns();
		if (runs.delete(runname.toUpperCase())) {
			System.out.println("Run " + runname + " deleted");
		} else {
			System.out.println("Run " + runname + " not found");
		}

		return null;
	}
}
