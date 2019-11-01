/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.run;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IFrameworkRuns;
import dev.galasa.framework.spi.IRun;

@Command(scope = "run", name = "list", description = "List all runs")
@Service
public class RunList implements Action {

    @Override
    public Object execute() throws Exception {

        final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

        if (!devEnv.isFrameworkInitialised()) {
            System.err.println("The Framework has not been initialised, use cirillo:init");
            return null;
        }

        IFrameworkRuns frameworkRuns = devEnv.getFramework().getFrameworkRuns();
        List<IRun> runs = frameworkRuns.getAllRuns();
        Collections.sort(runs, new RunSort());

        for (IRun run : runs) {
            System.out.println(run.getName() + " - " + run.getStatus());
        }

        return null;
    }

    private static class RunSort implements Comparator<IRun> {

        @Override
        public int compare(IRun o1, IRun o2) {
            return o1.getName().compareTo(o2.getName());
        }

    }
}
