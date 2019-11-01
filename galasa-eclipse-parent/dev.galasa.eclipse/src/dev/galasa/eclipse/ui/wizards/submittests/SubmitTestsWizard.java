/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.preferences.PreferenceConstants;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestClass;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestStream;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class SubmitTestsWizard extends Wizard implements INewWizard {

    private IConfigurationPropertyStoreService cps;

    private SelectTestsWizardPage              selectionWizard;
    private ConfigurationWizardPage            configurationWizardPage;

    public SubmitTestsWizard() throws ConfigurationPropertyStoreException, FrameworkException {
        cps = Activator.getInstance().getFramework().getConfigurationPropertyService("framework");

        selectionWizard = new SelectTestsWizardPage("select", cps);
        configurationWizardPage = new ConfigurationWizardPage("config", cps);

        setWindowTitle("Submit automation test runs");
    }

    @Override
    public void init(IWorkbench arg0, IStructuredSelection arg1) {

    }

    @Override
    public boolean performFinish() {

        TestStream testStream = selectionWizard.getTestStream();
        List<TestClass> selectedClasses = selectionWizard.getSelectedClasses();

        boolean trace = configurationWizardPage.isTrace();

        IFile overridesFile = configurationWizardPage.getOverridesFile();
        boolean includeGlobalOverrides = configurationWizardPage.isIncludeGlobalOverrides();

        IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
        String requestorId = preferenceStore.getString(PreferenceConstants.P_REQUESTOR_ID);
        if (requestorId.isEmpty()) {
            requestorId = preferenceStore.getDefaultString(PreferenceConstants.P_REQUESTOR_ID);
        }

        try {
            String mavenRepository = AbstractManager
                    .nulled(cps.getProperty("test.stream." + testStream.getId(), "maven.repo"));
            String obr = AbstractManager.nulled(cps.getProperty("test.stream." + testStream.getId(), "obr"));

            SubmitTestRunsJob job = new SubmitTestRunsJob(testStream, selectedClasses, trace, requestorId,
                    overridesFile, includeGlobalOverrides, mavenRepository, obr);
            job.schedule();
        } catch (FrameworkException e) {
            Activator.log(e);
        }

        return true;
    }

    @Override
    public void addPages() {
        addPage(selectionWizard);
        addPage(configurationWizardPage);
    }

}
