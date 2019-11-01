/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestStream;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IFramework;

public class FetchTestStreamsJob extends Job {

    private final SelectTestsWizardPage selectTestsWizardPage;

    public FetchTestStreamsJob(SelectTestsWizardPage selectTestsWizardPage) {
        super("Fetch Test Streams");

        this.selectTestsWizardPage = selectTestsWizardPage;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        IConfigurationPropertyStoreService cps = this.selectTestsWizardPage.getCPS();

        try {
            IFramework framework = Activator.getInstance().getFramework();
            if (!framework.isInitialised()) {
                return new Status(Status.OK, Activator.PLUGIN_ID,
                        "Test Streams not fetched - Framework not intialised");
            }

            String testStreams = AbstractManager.defaultString(cps.getProperty("test", "streams"),
                    "galasa-ivt,simframe");

            String[] splitTestStreams = testStreams.split(",");
            List<TestStream> streams = new ArrayList<>();
            for (String stream : splitTestStreams) {
                if (stream.trim().isEmpty()) {
                    continue;
                }

                streams.add(new TestStream(stream, cps));
            }

            this.selectTestsWizardPage.setTestStreams(streams);

        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Test Streams fetched");
    }

}
