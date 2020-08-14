/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.eclipse.launcher.tabs;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.launcher.GherkinLauncher;

public class GherkinConfigurationTab extends AbstractLaunchConfigurationTab
implements IConfigurationTab, SelectionListener, ModifyListener, FocusListener {

    private Composite                       comp;

    private FeatureSelectionWidget          featureSelector;

    private Button                          trace;
    private Button                          includeWorkspaceOverrides;
    private Text                            propertiesFile;
    private Button                          propertiesFileBrowse;
    private IFile                           propertiesFilePath;

    private ArrayList<IConfigurationGroups> configurationGroups = new ArrayList<>();

    public GherkinConfigurationTab() {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] extensions = extensionRegistry
                .getConfigurationElementsFor("dev.galasa.eclipse.extension.launcher.gherkin.configuration");
        for (IConfigurationElement extension : extensions) {
            try {
                configurationGroups.add((IConfigurationGroups) extension.createExecutableExtension("class"));
            } catch (CoreException e) {
                Activator.log(e);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        this.comp = new Composite(parent, SWT.NONE);

        GridLayout topLayout = new GridLayout(1, false);
        comp.setLayout(topLayout);

        createFeatureSelectionControls(comp);

        createOverridesControler(comp);

        createDiagnosticControler(comp);

        for (IConfigurationGroups group : configurationGroups) {
            group.createControl(this, comp);
        }

        Dialog.applyDialogFont(comp);

        setControl(comp);

        setErrorMessage(null);
    }

    @Override
    public String getName() {
        return "Galasa Gherkin Configuration";
    }

    /**
     * Create controls that allow the user to select which tests to run
     * 
     * @param parent
     */
    private void createFeatureSelectionControls(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Test Selection");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        group.setLayoutData(gd);

        GridLayout topLayout = new GridLayout(1, false);
        group.setLayout(topLayout);

        featureSelector = new FeatureSelectionWidget(getLaunchConfigurationDialog(), group, SWT.NONE);
        featureSelector.addModifyListener(this);
    }

    /**
     * Create controls that affects diagnostics
     * 
     * @param parent
     */
    private void createOverridesControler(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Override Properties");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        group.setLayoutData(gd);

        GridLayout topLayout = new GridLayout(1, false);
        group.setLayout(topLayout);

        Composite comp = new Composite(group, SWT.NONE);
        GridData compGD = new GridData();
        compGD.horizontalAlignment = SWT.FILL;
        compGD.grabExcessHorizontalSpace = true;
        comp.setLayoutData(compGD);

        GridLayout testpropsGL = new GridLayout(2, false);
        comp.setLayout(testpropsGL);

        propertiesFile = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
        GridData propertiesFileGD = new GridData();
        propertiesFileGD.grabExcessHorizontalSpace = true;
        propertiesFileGD.horizontalAlignment = SWT.FILL;
        propertiesFile.setLayoutData(propertiesFileGD);

        propertiesFileBrowse = new Button(comp, SWT.PUSH);
        propertiesFileBrowse.setText("Browse");
        propertiesFileBrowse.addSelectionListener(this);

        includeWorkspaceOverrides = new Button(group, SWT.CHECK);
        includeWorkspaceOverrides.setText("Include ~/.galasa/override.properties TODO");
        includeWorkspaceOverrides.addSelectionListener(this);
    }

    /**
     * Create controls that affects diagnostics
     * 
     * @param parent
     */
    private void createDiagnosticControler(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Diagnostics");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        group.setLayoutData(gd);

        GridLayout topLayout = new GridLayout(1, false);
        group.setLayout(topLayout);

        trace = new Button(group, SWT.CHECK);
        trace.setText("Write TRACE level messages");
        trace.addSelectionListener(this);
    }

    private void validatePage() {

        setMessage(null);
        setErrorMessage(null);

        // carry out test selection check
        if (!validateTestSelection()) {
            return;
        }

        if (propertiesFilePath != null) {
            if (!propertiesFilePath.exists()) {
                setErrorMessage("Override properties file is missing");
                return;
            }
        }

        for (IConfigurationGroups group : configurationGroups) {
            if (!group.validatePage()) {
                return;
            }
        }

        return;
    }

    private boolean validateTestSelection() {

        try {
            String sProject = featureSelector.getProjectText();
            if (sProject == null || sProject.trim().isEmpty()) {
                setErrorMessage("A test project must be provided");
                return false;
            }
            sProject = sProject.trim();

            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = workspaceRoot.getProject(sProject);
            if (project == null) {
                setErrorMessage("Test project is missing from the workspace");
                return false;
            }

            // *** Validate the Feature file
            String feature = featureSelector.getFeatureText();
            if (feature == null || feature.trim().isEmpty()) {
                setErrorMessage("A feature file must be provided");
                return false;
            }
            IFile file = project.getFile(feature);
            if (file == null || !file.exists()) {
                setErrorMessage("Unable to locate feature file");
                return false;
            }

        } catch (Exception e) {
            setErrorMessage("Problem validating test selection, see error log");
            Activator.log(e);
            return false;
        }

        //        if(automationRunTest.getSelection()) {
        //            String errorMessage = automationRunSelector.validate();
        //            if(errorMessage!=null) {
        //                setErrorMessage(errorMessage);
        //                return false;
        //            }
        //        }

        return true;
    }

    @Override
    public void initializeFrom(ILaunchConfiguration config) {
        String projectName = "";

        try {
            projectName = config.getAttribute(GherkinLauncher.PROJECT, "");
            trace.setSelection(config.getAttribute(GherkinLauncher.TRACE, false));
            includeWorkspaceOverrides.setSelection(config.getAttribute(GherkinLauncher.WORKSPACE_OVERRIDES, true));

            // set the project name
            featureSelector.setProjectText(projectName);
            featureSelector.setFeature(config.getAttribute(GherkinLauncher.FEATURE, ""));

            String path = config.getAttribute(GherkinLauncher.OVERRIDES, "");
            if (path.isEmpty()) {
                propertiesFile.setText("");
                propertiesFilePath = null;
            } else {
                org.eclipse.core.runtime.Path epath = new org.eclipse.core.runtime.Path(path);
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                propertiesFilePath = root.getFileForLocation(epath);
                propertiesFile.setText(propertiesFilePath.getName());
            }
        } catch (Exception e) {
            Activator.log(e);
        }

        for (IConfigurationGroups group : configurationGroups) {
            group.initializeFrom(config);
        }

        validatePage();

        return;
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
                configuration.setAttribute(GherkinLauncher.PROJECT, featureSelector.getProjectText());
                configuration.setAttribute(GherkinLauncher.FEATURE, featureSelector.getFeatureText());
        configuration.setAttribute(GherkinLauncher.TRACE, trace.getSelection());
        configuration.setAttribute(GherkinLauncher.WORKSPACE_OVERRIDES, includeWorkspaceOverrides.getSelection());
        if (propertiesFilePath == null) {
            configuration.setAttribute(GherkinLauncher.OVERRIDES, "");
        } else {
            configuration.setAttribute(GherkinLauncher.OVERRIDES, propertiesFilePath.getLocation().toString());
        }

        for (IConfigurationGroups group : configurationGroups) {
            group.performApply(configuration);
        }

    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(GherkinLauncher.PROJECT, "");
        config.setAttribute(GherkinLauncher.FEATURE, "");
        config.setAttribute(GherkinLauncher.TRACE, false);
        config.setAttribute(GherkinLauncher.WORKSPACE_OVERRIDES, true);
        config.setAttribute(GherkinLauncher.OVERRIDES, "");

        for (IConfigurationGroups group : configurationGroups) {
            group.setDefaults(config);
        }

    }

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
    }

    @Override
    public void modifyText(ModifyEvent arg0) {
        validatePage();
        updateLaunchConfigurationDialog();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        configurationUpdate();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == propertiesFileBrowse) {
            FilteredResourcesSelectionDialog d = new FilteredResourcesSelectionDialog(getShell(), false,
                    ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
            d.setInitialPattern("*.gover", FilteredResourcesSelectionDialog.NONE);
            d.setTitle("Select a properties file");
            if (d.open() == FilteredResourcesSelectionDialog.CANCEL) {
                return;
            }
            if (d.getFirstResult() != null) {
                propertiesFilePath = (IFile) d.getFirstResult();
                propertiesFile.setText(propertiesFilePath.getName());
            }
        }

        configurationUpdate();
    }

    @Override
    public void configurationUpdate() {
        setDirty(true);
        updateLaunchConfigurationDialog();
    }

}
