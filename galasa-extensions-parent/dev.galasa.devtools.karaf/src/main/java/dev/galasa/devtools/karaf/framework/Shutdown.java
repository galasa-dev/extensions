package dev.galasa.devtools.karaf.framework;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.ConsoleLog;
import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.Framework;

@Command(scope = "galasa", name = "shutdown", description = "Shutdown the Galasa Framework")
@Service
public class Shutdown implements Action {

    @Override
    public Object execute() throws Exception {
    	
    	final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();
    	
    	Framework framework = (Framework) devEnv.getFramework();
    	if (framework.isShutdown()) {
    		System.out.print("The Galasa Framework is already shutdown");
    		return null;
    	}
    	
    	System.out.println("About to shutdown the Galasa Framework");
    	
    	ConsoleLog log = new ConsoleLog();
    	
    	framework.shutdown(log);
    	
        return null;
    }
}
