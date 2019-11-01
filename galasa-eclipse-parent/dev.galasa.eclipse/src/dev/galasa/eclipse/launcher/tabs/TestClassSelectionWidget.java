/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.launcher.tabs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.ui.dialogs.SelectionDialog;

import dev.galasa.eclipse.Activator;

/**
 * Widget for Test class selection
 * 
 * @author Michael Baylis
 *
 */
public class TestClassSelectionWidget {

    /**
     * Text area for the name of the test project
     */
    private Text             projectText;

    /**
     * Button used to select a project for the test
     */
    private Button           projectSelectBtn;

    /**
     * Text area for the name of the test class to be selected
     */
    private Text             classText;

    /**
     * Button used to select a test class from the project
     */
    private Button           classSelectBtn;

    /**
     * Text area for test method name
     * 
     * <p>
     * By default disabled, but may contain name of a specific test method to run
     * </p>
     */
    private Text             methodText;

    /**
     * Button for selecting a test method
     * 
     */
    private Button           methodSelectBtn;

    /**
     * Context for the search engine to run within
     * 
     */
    private IRunnableContext context;

    /**
     * 
     * @param context
     * @param parent
     * @param style
     */
    public TestClassSelectionWidget(IRunnableContext context, Composite parent, int style) {
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
        classText.setEnabled(enabled);
        classSelectBtn.setEnabled(enabled);
        methodText.setEnabled(enabled);
        methodSelectBtn.setEnabled(enabled);

    }

    /**
     * Allow the launch config tab to hook into text fields for validation purposes
     * 
     * @param listener
     */
    public void addModifyListener(ModifyListener listener) {
        projectText.addModifyListener(listener);
        classText.addModifyListener(listener);
        methodText.addModifyListener(listener);
    }

    /**
     * Allow the launch config tab to remove itself as a listener
     * 
     * @param listener
     */
    public void removeModifyListener(ModifyListener listener) {
        projectText.removeModifyListener(listener);
        classText.removeModifyListener(listener);
        methodText.removeModifyListener(listener);
    }

    /*
     * Show a dialog that lets the user select a project. This in turn provides
     * context for the main type, allowing the user to key a main type name, or
     * constraining the search for main types to the specified project.
     */
    private void handleProjectButtonSelected() {
        IJavaProject project = chooseJavaProject();
        if (project == null) {
            return;
        }

        String projectName = project.getElementName();
        projectText.setText(projectName);
        classText.setText("");
        classText.setEnabled(true);
        classSelectBtn.setEnabled(true);
        methodText.setText("");
        methodText.setEnabled(false);
        methodSelectBtn.setEnabled(false);
    }

    private String chooseMethodName(Set<String> methodNames) {
        Shell shell = Display.getDefault().getActiveShell();

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
        dialog.setMessage("Select test method from test class " + classText.getText());
        dialog.setTitle("Select test method");

        int methodCount = methodNames.size();
        String[] elements = new String[methodCount + 1];
        methodNames.toArray(elements);
        elements[methodCount] = "(all methods)";

        dialog.setElements(elements);

        String methodName = methodText.getText();

        if (methodNames.contains(methodName)) {
            dialog.setInitialSelections(new String[] { methodName });
        }

        dialog.setAllowDuplicates(false);
        dialog.setMultipleSelection(false);
        if (dialog.open() == Window.OK) {
            String result = (String) dialog.getFirstResult();
            return (result == null || result.equals("(all methods)")) ? "" : result; //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Show a dialog that allows the user to select a test method to run
     * 
     */
    private void handleTestMethodSearchButtonSelected() {
        try {

            Set<String> methodNames = getMethodsForType(getTestClass());

            String methodName = chooseMethodName(methodNames);

            if (methodName != null) {
                methodText.setText(methodName);

            }
        } catch (JavaModelException e) {
            Activator.log(e.getStatus());
        }
    }

    private Set<String> getMethodsForType(IType type) throws JavaModelException {

        HashSet<String> result = new HashSet<String>();

        for (IMethod method : type.getMethods()) {
            // list all method annotations
            if (method.getAnnotation("Test").exists())
                result.add(method.getElementName());
        }

        return result;
    }

    /**
     * Realize a Java Project selection dialog and return the first selected
     * project, or null if there was none.
     */
    private IJavaProject chooseJavaProject() {
        IJavaProject[] projects;

        try {
            projects = JavaCore.create(getWorkspaceRoot()).getJavaProjects();
        } catch (JavaModelException e) {
            Activator.log(e.getStatus());
            projects = new IJavaProject[0];
        }

        ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault().getActiveShell(),
                labelProvider);
        dialog.setTitle("Project Selection");
        dialog.setMessage("Choose a project to constrain the search for test classes");
        dialog.setElements(projects);

        IJavaProject javaProject = getJavaProject();
        if (javaProject != null) {
            dialog.setInitialSelections(new Object[] { javaProject });
        }
        if (dialog.open() == Window.OK) {
            return (IJavaProject) dialog.getFirstResult();
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
        classLabel.setText("Test class:");

        classText = new Text(comp, SWT.BORDER);
        classText.setEnabled(false);
        classText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                handleFieldUpdates();
            }
        });

        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        classText.setLayoutData(gd);

        classSelectBtn = new Button(comp, SWT.PUSH);
        classSelectBtn.setText("Search...");
        classSelectBtn.setEnabled(false);

        classSelectBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleClassSearchButtonSelected();
            }
        });

        // create method selection triplet
        Label methodLabel = new Label(comp, SWT.NONE);
        methodLabel.setText("Test method:");

        methodText = new Text(comp, SWT.BORDER);
        methodText.setEnabled(false);
        methodText.setText("(all methods)");
        methodText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                handleFieldUpdates();
            }
        });

        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;

        methodText.setLayoutData(gd);

        methodSelectBtn = new Button(comp, SWT.PUSH);
        methodSelectBtn.setText("Search...");
        methodSelectBtn.setEnabled(false);

        methodSelectBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleTestMethodSearchButtonSelected();
                handleFieldUpdates();
            }
        });

        // disable for now
        methodText.setEnabled(false);
        methodSelectBtn.setEnabled(false);
    }

    /**
     * Handle the invocation of the test selection dialog
     */
    private void handleClassSearchButtonSelected() {
        Shell shell = Display.getDefault().getActiveShell();

        IJavaProject javaProject = getJavaProject();
        IType[] types = TestSearchEngine.findTests(getJavaProject());

        final HashSet<String> typeLookup = new HashSet<String>();

        for (IType type : types) {
            typeLookup.add(type.getPackageFragment().getElementName() + '/' + type.getTypeQualifiedName('.'));
        }

        SelectionDialog dialog = null;
        try {

            dialog = JavaUI.createTypeDialog(shell, context,
                    SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }, IJavaSearchScope.SOURCES),
                    IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false, "**", //$NON-NLS-1$
                    new TypeSelectionExtension() {
                        @Override
                        public ITypeInfoFilterExtension getFilterExtension() {
                            return new ITypeInfoFilterExtension() {

                                public boolean select(ITypeInfoRequestor requestor) {
                                    StringBuffer buf = new StringBuffer();
                                    buf.append(requestor.getPackageName()).append('/');
                                    String enclosingName = requestor.getEnclosingName();
                                    if (enclosingName.length() > 0)
                                        buf.append(enclosingName).append('.');
                                    buf.append(requestor.getTypeName());
                                    return typeLookup.contains(buf.toString());
                                }
                            };
                        }
                    });
        } catch (JavaModelException e) {
            Activator.log(e);
            return;
        }

        dialog.setTitle("Test Selection ");
        dialog.setMessage("Choose a test case or test suite:  ");
        if (dialog.open() == Window.CANCEL) {
            return;
        }

        Object[] results = dialog.getResult();
        if ((results == null) || (results.length < 1)) {
            return;
        }
        IType type = (IType) results[0];

        if (type != null) {
            setClass(type.getFullyQualifiedName('.'));
            javaProject = type.getJavaProject();
            setProjectText(javaProject.getElementName());
        }

    }

    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    /*
     * Return the IJavaProject corresponding to the project name in the project name
     * text field, or null if the text does not match a project name.
     */
    private IJavaProject getJavaProject() {
        String projectName = projectText.getText().trim();
        if (projectName.length() < 1) {
            return null;
        }
        return JavaCore.create(getWorkspaceRoot()).getJavaProject(projectName);
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
    private IType getTestClass() {

        IJavaProject javaProject = getJavaProject();
        if (javaProject == null)
            return null;

        try {
            return javaProject.findType(classText.getText().trim());
        } catch (JavaModelException e) {
            Activator.log(e);
            return null;
        }
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
     * Returns a text representation of the class and method
     * 
     * @return
     *         <p>
     *         fully qualified class name, a '#' character and the name of the
     *         method
     */
    public String getMethodText() {
        if (methodText.isEnabled() && methodText.getText().length() > 1
                && !methodText.getText().equals("(all methods)"))
            return getClassText() + "#" + methodText.getText();
        else
            return null;
    }

    /**
     * 
     * @return - content of the 'class' text field
     */
    public String getClassText() {
        return classText.getText();
    }

    /**
     * Set the test class name
     * 
     * @param klass
     */
    public void setClass(String klass) {
        classText.setText(klass);
        methodText.setText("(all methods)");
        methodText.setEnabled(true);
        methodSelectBtn.setEnabled(true);
    }

    /**
     * Set the method and class field with given method name
     * 
     * @param method
     */
    public void setMethod(String method) {

        String className = method.substring(0, method.indexOf("#"));
        String methodName = method.substring(method.indexOf("#") + 1, method.length());
        setClass(className);
        methodText.setText(methodName);
        methodText.setEnabled(true);
        methodSelectBtn.setEnabled(true);
    }

    /**
     * Method is triggered when the test project, class or method are updated
     */
    public void handleFieldUpdates() {

        boolean classEnabled = getJavaProject() != null;
        boolean methodEnabled = getTestClass() != null && classEnabled;

        classText.setEnabled(classEnabled);
        classSelectBtn.setEnabled(classEnabled);
        methodText.setEnabled(methodEnabled);
        methodSelectBtn.setEnabled(methodEnabled);

    }

}
