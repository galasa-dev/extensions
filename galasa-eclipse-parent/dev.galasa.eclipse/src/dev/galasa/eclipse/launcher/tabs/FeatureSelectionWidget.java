/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.eclipse.launcher.tabs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

/**
 * Widget for Test class selection
 * 
 * @author Michael Baylis
 *
 */
public class FeatureSelectionWidget {

    /**
     * Text area for the name of the test project
     */
    private Text             projectText;

    /**
     * Button used to select a project for the test
     */
    private Button           projectSelectBtn;

    /**
     * Text area for the name of the feature file to be selected
     */
    private Text             featureText;

    /**
     * Button used to select a feature file from the project
     */
    private Button           featureSelectBtn;

    /**
     * Context for the search engine to run within
     * 
     */
    @SuppressWarnings("unused")
    private IRunnableContext context;

    /**
     * 
     * @param context
     * @param parent
     * @param style
     */
    public FeatureSelectionWidget(IRunnableContext context, Composite parent, int style) {
        this.context = context;
        createControls(parent);
    }

    /**
     * Disable/enable the controls relating to class and test method selection
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        projectText.setEnabled(enabled);
        projectSelectBtn.setEnabled(enabled);
        featureText.setEnabled(enabled);
        featureSelectBtn.setEnabled(enabled);
    }

    /**
     * Allow the launch config tab to hook into text fields for validation purposes
     * 
     * @param listener
     */
    public void addModifyListener(ModifyListener listener) {
        projectText.addModifyListener(listener);
        featureText.addModifyListener(listener);
    }

    /**
     * Allow the launch config tab to remove itself as a listener
     * 
     * @param listener
     */
    public void removeModifyListener(ModifyListener listener) {
        projectText.removeModifyListener(listener);
        featureText.removeModifyListener(listener);
    }

    /*
     * Show a dialog that lets the user select a project. This in turn provides
     * context for the main type, allowing the user to key a main type name, or
     * constraining the search for main types to the specified project.
     */
    private void handleProjectButtonSelected() {
        IProject project = chooseProject();
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        projectText.setText(projectName);
        featureText.setText("");
        featureText.setEnabled(true);
        featureSelectBtn.setEnabled(true);
    }

    /**
     * Realize a Project selection dialog and return the first selected
     * project, or null if there was none.
     */
    private IProject chooseProject() {
        IProject[] projects = getWorkspaceRoot().getProjects();

        ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault().getActiveShell(),
                labelProvider);
        dialog.setTitle("Project Selection");
        dialog.setMessage("Choose a project to constrain the search for feature files");
        dialog.setElements(projects);

        IProject project = getProject();
        if (project != null) {
            dialog.setInitialSelections(new Object[] { project });
        }
        if (dialog.open() == Window.OK) {
            return (IProject) dialog.getFirstResult();
        }
        return null;
    }

    /**
     * Set up the layout for this composite object
     */
    private void createControls(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridData gd = new GridData();

        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        comp.setLayoutData(gd);

        // create project selection triplet
        GridLayout topLayout = new GridLayout(3, false);
        comp.setLayout(topLayout);

        Label projectLabel = new Label(comp, SWT.NONE);
        projectLabel.setText("Project: ");

        projectText = new Text(comp, SWT.BORDER);

        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        projectText.setLayoutData(gd);
        projectText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                handleFieldUpdates();
            }
        });

        projectSelectBtn = new Button(comp, SWT.PUSH);
        projectSelectBtn.setText("Browse...");
        projectSelectBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleProjectButtonSelected();
            }
        });

        // create class selection triplet
        Label classLabel = new Label(comp, SWT.NONE);
        classLabel.setText("Feature file:");

        featureText = new Text(comp, SWT.BORDER);
        featureText.setEnabled(false);
        featureText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                handleFieldUpdates();
            }
        });

        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        featureText.setLayoutData(gd);

        featureSelectBtn = new Button(comp, SWT.PUSH);
        featureSelectBtn.setText("Search...");
        featureSelectBtn.setEnabled(false);

        featureSelectBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleFeatureSearchButtonSelected();
            }
        });

    }

    /**
     * Handle the invocation of the test selection dialog
     */
    private void handleFeatureSearchButtonSelected() {
        Shell shell = Display.getDefault().getActiveShell();

        IProject project = getProject();
        if (project == null) {
            return;
        }

        FilteredResourcesSelectionDialog d = new FilteredResourcesSelectionDialog(shell, false,
                project, IResource.FILE);
        d.setInitialPattern("*.feature", FilteredResourcesSelectionDialog.NONE);
        d.setTitle("Select a properties file");
        if (d.open() == FilteredResourcesSelectionDialog.CANCEL) {
            return;
        }

        IFile x = (IFile) d.getFirstResult();
        setFeature(x.getProjectRelativePath().toString());

    }




    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    /*
     * Return the IProject corresponding to the project name in the project name
     * text field, or null if the text does not match a project name.
     */
    private IProject getProject() {
        String projectName = projectText.getText().trim();
        if (projectName.length() < 1) {
            return null;
        }
        return getWorkspaceRoot().getProject(projectName);
    }

    /**
     * Get the IType of the test class in the classText field
     * 
     * @return
     *         <p>
     *         The IType representative of the test class to be run
     *         </p>
     * @throws JavaModelException
     */
    private IFile getFeature() {

        IProject project = getProject();
        if (project == null)
            return null;

        return project.getFile(featureText.getText().trim());
    }

    /**
     * 
     * @return - content of the 'project' text field
     */
    public String getProjectText() {
        return projectText.getText();
    }

    /**
     * Set content of the 'project' text field
     * 
     * @param text
     */
    public void setProjectText(String text) {
        projectText.setText(text);
    }

    /**
     * 
     * @return - content of the 'class' text field
     */
    public String getFeatureText() {
        return featureText.getText().trim();
    }

    /**
     * Set the test class name
     * 
     * @param klass
     */
    public void setFeature(String feature) {
        featureText.setText(feature);
    }

    /**
     * Method is triggered when the test project, class or method are updated
     */
    public void handleFieldUpdates() {

        boolean featureEnabled = getProject() != null;

        featureText.setEnabled(featureEnabled);
        featureSelectBtn.setEnabled(featureEnabled);
    }

}
