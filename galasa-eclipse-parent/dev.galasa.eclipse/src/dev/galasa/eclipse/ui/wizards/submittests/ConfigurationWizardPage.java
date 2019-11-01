/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class ConfigurationWizardPage extends WizardPage implements SelectionListener {

    private final IConfigurationPropertyStoreService cps;

    private Button                                   includeWorkspaceOverrides;
    private Text                                     propertiesFile;
    private Button                                   propertiesFileBrowse;
    private IFile                                    propertiesFilePath;

    @SuppressWarnings("unused")
    private boolean                                  disposed = false;
    private Button                                   trace;

    public ConfigurationWizardPage(String pageName, IConfigurationPropertyStoreService cps) {
        super(pageName);

        this.cps = cps;

        setTitle("Run Configuration");
        setDescription("Choose the configuration the test will run with.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite master = new Composite(parent, SWT.NONE);
        master.setLayout(new GridLayout(1, false));
        master.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createOverridesControler(master); // TODO create composite class with ConfigurationTab
        createDiagnosticControler(master);

        setControl(master);
    }

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
        includeWorkspaceOverrides.setSelection(false);
        includeWorkspaceOverrides.addSelectionListener(this);

        validatePage();
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

    @Override
    public void dispose() {
        this.disposed = true;
        super.dispose();
    }

    private void validatePage() {
        setMessage(null);
        setErrorMessage(null);

        if (propertiesFilePath != null) {
            if (!propertiesFilePath.exists()) {
                setErrorMessage("Override properties file is missing");
                return;
            }
        }

        return;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.getSource() == propertiesFileBrowse) {
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
    }

    public IConfigurationPropertyStoreService getCPS() {
        return this.cps;
    }

    public boolean isTrace() {
        return trace.getSelection();
    }

    public IFile getOverridesFile() {
        return propertiesFilePath;
    }

    public boolean isIncludeGlobalOverrides() {
        return includeWorkspaceOverrides.getSelection();
    }

}
