/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.ras;

import java.util.Collections;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.framework.spi.IResultArchiveStore;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;

@Command(scope = "ras", name = "tests", description = "List the different tests in the RAS")
@Service
public class RasTests implements Action {

	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}

		IResultArchiveStore ras = devEnv.getFramework().getResultArchiveStore();
		List<IResultArchiveStoreDirectoryService> rasDir = ras.getDirectoryServices();
		
		List<String> tests = rasDir.get(0).getTests();
		
		//*** Sort by test name
		Collections.sort(tests);
		
		for(String test : tests) {
			System.out.println(test);
		}
		
		return null;
	}

}
