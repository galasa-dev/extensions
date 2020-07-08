/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.progress.IProgressConstants;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.preferences.PreferenceConstants;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestClass;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestStream;
import dev.galasa.framework.spi.IFrameworkRuns;
import dev.galasa.framework.spi.IRun;

public class SubmitTestRunsJob extends Job {

    private final TestStream      testStream;
    private final List<TestClass> selectedClasses;
    private final boolean         trace;
    private final String          requestorId;
    private final IFile           overridesFile;
    private final boolean         includeGlobalOverrides;
    private final String          mavenRepository;
    private final String          obr;

    public SubmitTestRunsJob(TestStream testStream, List<TestClass> selectedClasses, boolean trace, String requestorId,
            IFile overridesFile, boolean includeGlobalOverrides, String mavenRepository, String obr) {
        super("Submit Test runs");

        this.testStream = testStream;
        this.selectedClasses = selectedClasses;
        this.trace = trace;
        this.requestorId = requestorId;
        this.overridesFile = overridesFile;
        this.includeGlobalOverrides = includeGlobalOverrides;
        this.mavenRepository = mavenRepository;
        this.obr = obr;

        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
            String overrideUri = preferenceStore.getString(PreferenceConstants.P_OVERRIDES_URI);

            // *** Calculate which overrides file to use
            Properties generatedOverrides = new Properties();

            if (includeGlobalOverrides) {
                java.nio.file.Path workspaceOverridesFile = Paths.get(overrideUri);
                if (Files.exists(workspaceOverridesFile)) {
                    Properties p = new Properties();
                    p.load(Files.newInputStream(workspaceOverridesFile));

                    generatedOverrides.putAll(p);
                }
            }

            if (overridesFile != null) {
                java.nio.file.Path configOverridesFile = Paths.get(overridesFile.getLocationURI());
                if (Files.exists(configOverridesFile)) {
                    Properties p = new Properties();
                    p.load(Files.newInputStream(configOverridesFile));

                    generatedOverrides.putAll(p);
                }
            }

            IFrameworkRuns runs = Activator.getInstance().getFramework().getFrameworkRuns();

            StringBuilder sb = new StringBuilder();

            for (TestClass klass : selectedClasses) {
                IRun run = runs.submitRun("request", // TODO provide option
                        requestorId, klass.getBundle(), klass.getName(), null, // TODO provide a means to enter a group
                                                                               // name or default
                        mavenRepository, obr, testStream.getId(), false, trace, generatedOverrides, null, null,
                        "java");

                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(run.getName());
            }
            if (selectedClasses.size() == 1) {
                this.setName("Submitted run " + sb.toString());
            } else {
                this.setName("Submitted runs " + sb.toString());
            }
            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        } catch (Exception e) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed", e);
        }

        return new Status(Status.OK, Activator.PLUGIN_ID, "Submit worked");
    }

}
