/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.framework;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;

@Command(scope = "galasa", name = "status", description = "Galasa Framework Status")
@Service
public class Status implements Action {

    @Override
    public Object execute() throws Exception {

        final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

        if (devEnv.isFrameworkInitialised()) {
            System.out.println("The Galasa Framework is initialised");
        } else {
            System.out.println("The Galasa Framework is not initialised");
        }

        return null;
    }
}
