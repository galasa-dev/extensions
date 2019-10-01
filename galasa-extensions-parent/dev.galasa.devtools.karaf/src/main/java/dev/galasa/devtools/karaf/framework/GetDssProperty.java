package dev.galasa.devtools.karaf.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IDynamicStatusStoreService;

@Command(scope = "dss", name = "get", description = "Get DSS Property")
@Service
public class GetDssProperty implements Action {

    @Argument(index = 0, name = "property", description = "The property to use", required = true)
    private String    property;

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

        IDynamicStatusStoreService dss = devEnv.getDSS();

        if (this.namespace != null) {
            dss = devEnv.getFramework().getDynamicStatusStoreService(this.namespace);
        }

        if (dss == null) {
            this.logger.error("A namespace has not been selected, use -n or ejat:namespace");
            return null;
        }

        final String value = dss.get(this.property);
        if (value == null) {
            this.logger.info("Property not found");
            return null;
        }

        this.logger.info("Property found = '" + value + "'");

        return null;
    }
}
