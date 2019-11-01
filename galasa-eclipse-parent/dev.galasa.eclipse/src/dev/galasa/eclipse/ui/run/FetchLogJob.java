/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.IRunResult;

public class FetchLogJob extends Job {

    private final LogPage    logPage;
    private final IRunResult runResult;

    public FetchLogJob(LogPage logPage, IRunResult runResult) {
        super("Fetch Run Log");

        this.logPage = logPage;
        this.runResult = runResult;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            String log = this.runResult.getLog();
            this.logPage.setLog(log);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logPage.setLog("Fetch of run Log failed\n" + sw.toString());

            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Run Log Fetched");
    }

}
