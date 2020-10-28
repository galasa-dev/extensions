/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2020.
 */
package dev.galasa.eclipse.ui.results;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.IRunResultsListener;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ras.IRasSearchCriteria;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedFrom;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedTo;
import dev.galasa.framework.spi.ras.RasSearchCriteriaRequestor;
import dev.galasa.framework.spi.ras.RasSearchCriteriaTestName;

public class FetchRunsJob extends Job {

    private final IRunResultsListener                 listener;
    private final IResultArchiveStoreDirectoryService dirService;
    private final String                              requestor;
    private final String                              testClass;
    private final Instant                             from;
    private final Instant                             to;

    public FetchRunsJob(String requestor, String testClass, Instant from, Instant to,
            IResultArchiveStoreDirectoryService dirService, IRunResultsListener listener) {
        super("Fetch Runs");

        this.listener = listener;
        this.dirService = dirService;
        this.requestor = requestor;
        this.testClass = testClass;
        this.from = from;
        this.to = to;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            IFramework framework = Activator.getInstance().getFramework();
            if (!framework.isInitialised()) {
                return new Status(Status.OK, Activator.PLUGIN_ID, "Runs not fetched - Framework not intialised");
            }

            ArrayList<IRasSearchCriteria> criteria = new ArrayList<>();
            if (this.requestor != null) {
                criteria.add(new RasSearchCriteriaRequestor(this.requestor));
            }
            if (this.testClass != null) {
                criteria.add(new RasSearchCriteriaTestName(this.testClass));
            }
            if (this.from != null) {
                criteria.add(new RasSearchCriteriaQueuedFrom(this.from));
            }
            if (this.to != null) {
                criteria.add(new RasSearchCriteriaQueuedTo(this.to));
            }
            
            List<IRunResult> runs = dirService.getRuns(criteria.toArray(new IRasSearchCriteria[criteria.size()]));

            listener.runsUpdate(runs);
        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Runs fetched");
    }

}
