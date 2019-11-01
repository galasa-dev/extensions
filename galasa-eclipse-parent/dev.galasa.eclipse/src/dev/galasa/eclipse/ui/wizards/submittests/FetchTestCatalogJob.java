/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestCatalog;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestStream;

public class FetchTestCatalogJob extends Job {

    private final SelectTestsWizardPage selectTestsWizardPage;
    private final TestStream            testStream;

    public FetchTestCatalogJob(SelectTestsWizardPage selectTestsWizardPage, TestStream testStream) {
        super("Fetch Test Catalog");

        this.selectTestsWizardPage = selectTestsWizardPage;
        this.testStream = testStream;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            URI testCatalogURI = testStream.getLocation();
            if (testCatalogURI == null) {
                return new Status(Status.ERROR, Activator.PLUGIN_ID, "Unable to retrieve test catalog for stream "
                        + testStream.getId() + " as location is not known");
            }

            URLConnection connection = testCatalogURI.toURL().openConnection();
            Gson gson = new Gson();
            JsonObject jsonCatalog = gson.fromJson(new InputStreamReader(connection.getInputStream()),
                    JsonObject.class);

            TestCatalog testCatalog = new TestCatalog(jsonCatalog);

            this.selectTestsWizardPage.setTestCatalog(this.testStream, testCatalog);

        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Test Catalog fetched");
    }

}
