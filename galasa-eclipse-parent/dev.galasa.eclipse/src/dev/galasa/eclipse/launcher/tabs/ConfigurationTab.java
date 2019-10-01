package dev.galasa.eclipse.launcher.tabs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
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
import dev.galasa.eclipse.launcher.Launcher;

public class ConfigurationTab extends AbstractLaunchConfigurationTab implements SelectionListener, ModifyListener, FocusListener {

	private static final String MAVEN_NATURE = "org.eclipse.m2e.core.maven2Nature";

	private Composite comp;
	
	private TestClassSelectionWidget testClassSelector;
	
	private Button trace;
	private Button includeWorkspaceOverrides;
	private Text propertiesFile;
	private Button propertiesFileBrowse;
	private IFile propertiesFilePath;

	@Override
	public void createControl(Composite parent) {
		this.comp = new Composite(parent, SWT.NONE);

		GridLayout topLayout = new GridLayout(1, false);
		comp.setLayout(topLayout);

		createTestSelectionControls(comp);
		
		createOverridesControler(comp);

		createDiagnosticControler(comp);

		Dialog.applyDialogFont(comp);

		setControl(comp);

		setErrorMessage(null);
	}

	@Override
	public String getName() {
		return "Galasa Configuration";
	}

	/**
	 * Create controls that allow the user to select which tests to run
	 * 
	 * @param parent
	 */
	private void createTestSelectionControls(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Test Selection");
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		group.setLayoutData(gd);

		GridLayout topLayout = new GridLayout(1, false);
		group.setLayout(topLayout);

		testClassSelector = new TestClassSelectionWidget(
				getLaunchConfigurationDialog(), group, SWT.NONE);
		testClassSelector.addModifyListener(this);
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

		return;
	}

	private boolean validateTestSelection() {

		try {
			String sProject = testClassSelector.getProjectText();
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

			if (!project.hasNature(MAVEN_NATURE)) {
				setErrorMessage("Test project is not a Maven project");
				return false;
			}
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				setErrorMessage("Test project is not a Java project");
				return false;
			}

			IMavenProjectFacade mavenProjectFacade = MavenPlugin.getMavenProjectRegistry().getProject(project);
			IPath outputPath = mavenProjectFacade.getOutputLocation();
			if (outputPath == null) {
				setErrorMessage("Test project does have a build directory");
				return false;
			}

			IResource actualOutputPath = workspaceRoot.findMember(outputPath);
			if (actualOutputPath == null) {
				setErrorMessage("Test project build directory does not exist");
				return false;
			}
			Path projectDirectory = Paths.get(actualOutputPath.getRawLocationURI());
			Path manifestFile = projectDirectory.resolve("META-INF").resolve("MANIFEST.MF");
			if (!Files.exists(manifestFile)) {
				setErrorMessage("Test project does not a META-INF/MANIFEST.MF built");
				return false;
			}


			//*** Validate the Class
			String sClass = testClassSelector.getClassText();
			if (sClass == null || sClass.trim().isEmpty()) {
				setErrorMessage("A test class must be provided");
				return false;
			}
			sProject = sProject.trim();

			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject == null) {
				setErrorMessage("Test project did not resolve to a java project");
				return false;
			}

			IType type = javaProject.findType(sClass);
			if (type == null || !type.isClass()) {
				setErrorMessage("Unable to locate test class");
				return false;
			}

		} catch(Exception e) {
			setErrorMessage("Problem validating test selection, see error log");
			Activator.log(e);
			return false;
		}


		//		if(automationRunTest.getSelection()) {
		//			String errorMessage = automationRunSelector.validate();
		//			if(errorMessage!=null) {
		//				setErrorMessage(errorMessage);
		//				return false;
		//			}
		//		}

		return true;
	}


	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		String projectName = "";

		try {
			projectName = config.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			trace.setSelection(config.getAttribute(Launcher.TRACE, false));
			includeWorkspaceOverrides.setSelection(config.getAttribute(Launcher.WORKSPACE_OVERRIDES, true));

			// set the project name
			testClassSelector.setProjectText(projectName);
			testClassSelector.setClass(config.getAttribute(
					Launcher.TEST_CLASS, ""));
			
			String path = config.getAttribute(Launcher.OVERRIDES, "");
			if (path.isEmpty()) {
				propertiesFile.setText("");
				propertiesFilePath = null;
			} else {
				org.eclipse.core.runtime.Path epath = new org.eclipse.core.runtime.Path(path);
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				propertiesFilePath = root.getFileForLocation(epath);
				propertiesFile.setText(propertiesFilePath.getName());
			}
		} catch(Exception e) {
			Activator.log(e);
		}

		validatePage();

		return;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				testClassSelector.getProjectText());
		configuration.setAttribute(
				Launcher.TEST_CLASS,
				testClassSelector.getClassText());
		configuration.setAttribute(Launcher.TRACE, trace.getSelection());
		configuration.setAttribute(Launcher.WORKSPACE_OVERRIDES, includeWorkspaceOverrides.getSelection());
		if (propertiesFilePath == null) {
			configuration.setAttribute(
					Launcher.OVERRIDES, "");
		} else {
			configuration.setAttribute(
					Launcher.OVERRIDES,
					propertiesFilePath.getLocation().toString());
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(Launcher.TEST_CLASS, "");
		config.setAttribute(Launcher.TRACE,false);
		config.setAttribute(Launcher.WORKSPACE_OVERRIDES,true);
		config.setAttribute(Launcher.OVERRIDES, "");

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
		setDirty(true);
		updateLaunchConfigurationDialog();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == propertiesFileBrowse) {
			FilteredResourcesSelectionDialog d = new FilteredResourcesSelectionDialog(
					getShell(), false,
					ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
			d.setInitialPattern("*.gover",
					FilteredResourcesSelectionDialog.NONE);
			d.setTitle("Select a properties file");
			if (d.open() == FilteredResourcesSelectionDialog.CANCEL) {
				return;
			}
			if (d.getFirstResult() != null) {
				propertiesFilePath = (IFile) d.getFirstResult();
				propertiesFile.setText(propertiesFilePath.getName());
			}
		}

		setDirty(true);
		updateLaunchConfigurationDialog();
	}

}
