/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.framework;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.ConsoleLog;
import dev.galasa.framework.FrameworkInitialisation;

@Command(scope = "galasa", name = "init", description = "Galasa Framework Initialisation")
@Service
public class Init implements Action {

    private static final String seperator = "-----------------------------------------------------------------";

    @Option(name = "-b", aliases = {
            "--bootstrap" }, description = "Bootstrap properties URL", required = false, multiValued = false)
    private URL                 bootstrap;

    @Option(name = "-o", aliases = {
            "--overrides" }, description = "Overrides properties URL", required = false, multiValued = false)
    private URL                 overrides;

    @Override
    public Object execute() throws Exception {

        System.out.println("Performing initialisation of the Galasa Framework");

        if (this.bootstrap == null) {
            this.bootstrap = new URL("file://" + System.getProperty("user.home") + "/.galasa/bootstrap.properties");
        }

        if (this.overrides == null) {
            this.overrides = new URL("file://" + System.getProperty("user.home") + "/.galasa/overrides.properties");
        }

        System.out.println(seperator);

        System.out.println("Loading bootstrap properties file from " + this.bootstrap.toString());

        final Properties bootstrapProperties = new Properties();
        try (InputStream bootInputStream = this.bootstrap.openStream()) {
            bootstrapProperties.load(bootInputStream);
        }

        System.out.println(seperator);

        System.out.println("Loading override properties file from " + this.overrides.toString());

        final Properties overrideProperties = new Properties();
        overrideProperties.put("framework.resultarchive.store.include.default.local", "true");
        try (InputStream overrideInputStream = this.overrides.openStream()) {
            overrideProperties.load(overrideInputStream);
        }

        // *** Report of the current bootstrap
        System.out.println(seperator);
        System.out.println("Current BootStrap properties are:-");
        bootstrapProperties.store(System.out, null);

        // *** Report of the current bootstrap
        System.out.println(seperator);
        System.out.println("Current Override properties are:-");
        overrideProperties.store(System.out, null);

        System.out.println(seperator);
        System.out.println("Initialising the Framework");
        // *** Initialise the framework object
        try {
            ConsoleLog log = new ConsoleLog();
            FrameworkInitialisation fi = new FrameworkInitialisation(bootstrapProperties, overrideProperties, false,
                    log);
            if (fi.getFramework().isInitialised()) {
                System.out.println("Framework initialised");
            } else {
                System.err.println("The Framework does not think it is initialised, but we didn't get any errors");
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Exception("Framework failed to initialise");
        }

        return null;
    }
}
