/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

@Command(scope = "cps", name = "get", description = "Get CPS Property")
@Service
public class GetCpsProperty implements Action {

    @Argument(index = 0, name = "prefix", description = "The property prefix to use", required = true)
    private String    prefix;

    @Argument(index = 1, name = "suffix", description = "The property suffix to use", required = true)
    private String    suffix;

    @Argument(index = 2, name = "infixes", description = "The property infixes to use", required = false, multiValued = true)
    private String[]  infixes;

    @Option(name = "-n", aliases = {
            "--namespace" }, description = "Namespace to perform the get in", required = false, multiValued = false)
    private String    namespace;

    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public Object execute() throws Exception {

        final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

        if (!devEnv.isFrameworkInitialised()) {
            this.logger.error("The Framework has not been initialised, use cirillo:init");
            return null;
        }

        IConfigurationPropertyStoreService cps = devEnv.getCPS();

        if (this.namespace != null) {
            cps = devEnv.getFramework().getConfigurationPropertyService(this.namespace);
        }

        if (cps == null) {
            this.logger.error("A namespace has not been selected, use -n or ejat:namespace");
            return null;
        }

        final String value = cps.getProperty(this.prefix, this.suffix, this.infixes);
        if (value == null) {
            final String[] availableKeys = cps.reportPropertyVariants(this.prefix, this.suffix, this.infixes);
            this.logger.info("Property not found, available to set are properties are:");
            for (final String key : availableKeys) {
                this.logger.info(key);
            }
            return null;
        }

        this.logger.info("Property found = '" + value + "'");

        return null;
    }
}
