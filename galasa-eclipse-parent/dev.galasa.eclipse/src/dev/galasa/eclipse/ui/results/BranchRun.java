/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import java.time.Instant;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.teststructure.TestStructure;

public class BranchRun extends Branch {

    private final IRunResult runResult;
    private TestStructure    testStructure;
    private Instant          queued;

    public BranchRun(ResultsView view, IResultArchiveStoreDirectoryService dirService, IRunResult runResult) {
        super(view);
        this.runResult = runResult;

        try {
            this.testStructure = this.runResult.getTestStructure();
        } catch (Throwable t) {
            Activator.log(t);
            this.testStructure = new TestStructure();
        }

        this.queued = this.testStructure.getQueued();
        if (this.queued == null) {
            this.queued = Instant.ofEpochSecond(0);
        }
    }

    @Override
    public String toString() {
        return testStructure.getRunName() + " - " + testStructure.getTestShortName();
    }

    public IRunResult getResult() {
        return runResult;
    }

    public String getRunName() {
        return testStructure.getRunName();
    }

    public Instant getQueued() {
        return this.queued;
    }

}
