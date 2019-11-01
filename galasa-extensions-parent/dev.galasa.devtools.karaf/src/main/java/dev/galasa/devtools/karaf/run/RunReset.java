/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.run;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IFrameworkRuns;

@Command(scope = "run", name = "reset", description = "Reset a test run")
@Service
public class RunReset implements Action {

    @Argument(index = 0, name = "runname", description = "The run to reset", required = true)
    private String runname;

    @Override
    public Object execute() throws Exception {

        final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

        if (!devEnv.isFrameworkInitialised()) {
            System.err.println("The Framework has not been initialised, use cirillo:init");
            return null;
        }

        IFrameworkRuns runs = devEnv.getFramework().getFrameworkRuns();
        if (runs.reset(runname.toUpperCase())) {
            System.out.println("Run " + runname + " reset");
        } else {
            System.out.println("Run " + runname + " not found");
        }

        return null;
    }
}
