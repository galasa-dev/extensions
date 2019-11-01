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
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;

@Command(scope = "galasa", name = "namespace", description = "Switch namespace")
@Service
public class Namespace implements Action {

    @Argument(index = 0, name = "namespace", description = "The namespace to switch to", required = true)
    private String    namespace;

    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public Object execute() throws Exception {
    	
    	final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();
    	
    	if (!devEnv.isFrameworkInitialised()) {
            this.logger.error("The Framework has not been initialised, use galasa:init");
            return null;
        }

        if ("?".equals(this.namespace)) {
            this.logger.info("Current namespace is '" + devEnv.getNamespace() + "'");
            return null;
        }
        
        devEnv.setNamespace(this.namespace);

        this.logger.info("Namespace set to '" + this.namespace + "'");

        return null;
    }
}
