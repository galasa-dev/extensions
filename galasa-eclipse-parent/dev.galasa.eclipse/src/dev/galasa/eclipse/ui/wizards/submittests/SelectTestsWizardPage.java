/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import dev.galasa.eclipse.ui.wizards.submittests.model.TestBranch;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestBundle;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestCatalog;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestClass;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestPackage;
import dev.galasa.eclipse.ui.wizards.submittests.model.TestStream;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class SelectTestsWizardPage extends WizardPage
        implements SelectionListener, ModifyListener, ISelectionChangedListener {

    private TreeViewer                               treeViewer;

    private Combo                                    comboStream;
    private Button                                   saveButton;
    private Button                                   loadButton;
    private Text                                     searchText;

    private Composite                                testClassFrame;
    private FormToolkit                              toolkit;
    private ScrolledForm                             scrolledForm;

    private final IConfigurationPropertyStoreService cps;

    private boolean                                  disposed           = false;

    private List<TestStream>                         testStreams;

    private TestStream                               selectedTestStream = null;

    public SelectTestsWizardPage(String pageName, IConfigurationPropertyStoreService cps) {
        super(pageName);

        this.cps = cps;

        setTitle("Select tests");
        setDescription("Select tests to submit to the automation system.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite master = new Composite(parent, SWT.NONE);
        master.setLayout(new GridLayout(2, false));
        master.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite composite = new Composite(master, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(composite, SWT.NONE);
        label.setText("Test Bundle Stream");

        comboStream = new Combo(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        comboStream.setLayoutData(gridData); // combo is drop down, fills the gridData
        comboStream.add("Loading......");
        comboStream.select(0);
        comboStream.setEnabled(false);

        Label label2 = new Label(composite, SWT.NONE);
        label2.setText("Search for Test Class (RegEx):");

        searchText = new Text(composite, SWT.SEARCH);
        searchText.setLayoutData(gridData);
        searchText.addModifyListener(this);
        searchText.setEnabled(false);

        gridData = new GridData(); // container for treeViewer
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 610;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;

        treeViewer = new TreeViewer(composite);
        treeViewer.getControl().setLayoutData(gridData);

        setControl(master);

        treeViewer.setContentProvider(new TestTreeContentProvider());
        treeViewer.setLabelProvider(new TestTreeLabelProvider());
//		treeViewer.setSorter(new ViewerSorter());

        ArrayList<String> loading = new ArrayList<String>(1);
        loading.add("Select test stream....");
        treeViewer.setInput(loading);
        treeViewer.addSelectionChangedListener(this);
        treeViewer.getControl().setEnabled(false);

//		saveButton = new Button(composite, SWT.NONE);
//		saveButton.setText("Save Test Selection...");
//		saveButton.addSelectionListener(this);
//
//		loadButton = new Button(composite, SWT.NONE);
//		loadButton.setText("Load Test Selection...");
//		loadButton.addSelectionListener(this);
//		
//		

        toolkit = new FormToolkit(parent.getDisplay());
        testClassFrame = new Composite(master, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        testClassFrame.setLayout(gl);
        GridData temp = new GridData();
        temp.widthHint = 400;
        temp.grabExcessVerticalSpace = true;
        temp.grabExcessHorizontalSpace = true;
        temp.horizontalAlignment = SWT.FILL;
        temp.verticalAlignment = SWT.FILL;
        testClassFrame.setLayoutData(temp);

        new FetchTestStreamsJob(this).schedule();

        validatePage();
    }

    @Override
    public void dispose() {
        this.disposed = true;
        super.dispose();
    }

    private void validatePage() {
        setPageComplete(isPageValid());

        return;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        displayTestClass((ITreeSelection) event.getSelection());

        validatePage();
    }

    @Override
    public void modifyText(ModifyEvent event) {
        System.out.println("modifyText=" + event);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.getSource() == comboStream) {
            selectTestStream(this.testStreams.get(comboStream.getSelectionIndex()));
        }
    }

    public IConfigurationPropertyStoreService getCPS() {
        return this.cps;
    }

    public void setTestStreams(List<TestStream> testStreams) {
        if (disposed) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    setTestStreams(testStreams);
                }
            });
            return;
        }

        this.testStreams = testStreams;

        comboStream.removeAll();

        for (TestStream testStream : this.testStreams) {
            comboStream.add(testStream.getDescription());
        }

        comboStream.setEnabled(true);
        comboStream.addSelectionListener(this);

        comboStream.select(0); // TODO select previous used stream

        selectTestStream(testStreams.get(0));

        validatePage();
    }

    private void selectTestStream(TestStream testStream) {
        ArrayList<String> loading = new ArrayList<String>(1);
        loading.add("Retrieving tests, please wait.....");
        treeViewer.setInput(loading);
        treeViewer.getControl().setEnabled(false);

        selectedTestStream = testStream;
        new FetchTestCatalogJob(this, testStream).schedule();
    }

    public void setTestCatalog(TestStream testStream, TestCatalog testCatalog) {
        if (disposed) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    setTestCatalog(testStream, testCatalog);
                }
            });
            return;
        }

        if (selectedTestStream != testStream) {
            return;
        }

        treeViewer.setInput(testCatalog);
        treeViewer.getControl().setEnabled(true);

        validatePage();

        return;
    }

    private void displayTestClass(ITreeSelection selection) {
        if (scrolledForm != null) {
            scrolledForm.dispose();
            scrolledForm = null;
        }

        if (selection == null) {
            return;
        }

        Object[] selected = selection.toArray();
        if (selected.length != 1) {
            return;
        }

        if (!(selected[0] instanceof TestClass)) {
            return;
        }

        TestClass tc = (TestClass) selected[0];

        scrolledForm = toolkit.createScrolledForm(testClassFrame);
        scrolledForm.setText(tc.getShortName());
        GridLayout gl = new GridLayout();
        scrolledForm.setLayout(gl);
        scrolledForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledForm.getBody().setLayout(gl);
        scrolledForm.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Section overviewSection = toolkit.createSection(scrolledForm.getBody(),
                Section.DESCRIPTION | Section.TITLE_BAR | Section.EXPANDED);
        overviewSection.setText("Overview");
        if (tc.getSummary() == null) {
            overviewSection.setDescription("No summary text provided");
        } else {
            overviewSection.setDescription(tc.getSummary());
        }
        overviewSection.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Composite overviewComp = toolkit.createComposite(overviewSection);
        overviewComp.setLayout(new GridLayout(2, false));
        overviewComp.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        toolkit.createLabel(overviewComp, "Bundle:");
        toolkit.createLabel(overviewComp, tc.getBundle());
        toolkit.createLabel(overviewComp, "Package:");
        toolkit.createLabel(overviewComp, tc.getPackageName());
        overviewSection.setClient(overviewComp);

//		if (tc.javadoc != null && !tc.javadoc.isEmpty()) {
//			Section javadocSection = toolkit.createSection(scrolledForm.getBody(), 
//					Section.TITLE_BAR|Section.EXPANDED);
//			javadocSection.setText("Java Doc");
//			javadocSection.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
//
//			Composite javadocComp = toolkit.createComposite(javadocSection);
//			javadocComp.setLayout(new GridLayout(1, false));
//			javadocComp.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
//
//			toolkit.createLabel(javadocComp, tc.javadoc, SWT.WRAP);
//
//			javadocSection.setClient(javadocComp);
//		}

        testClassFrame.layout();

        return;
    }

    private boolean isPageValid() {
        setErrorMessage(null);

        if (testStreams == null || testStreams.isEmpty()) {
            setErrorMessage("Test Streams are unavailable, no framework.test.streams property");
            return false;
        }

        ISelection selected = treeViewer.getSelection();
        if (!(selected instanceof ITreeSelection)) {
            setErrorMessage("Internal selection is not a ITreeSelection");
            return false;
        }

        boolean found = false;
        for (Object selectedObject : ((ITreeSelection) selected).toArray()) {
            if (selectedObject instanceof TestClass) {
                found = true;
                break;
            }
            if (selectedObject instanceof TestBundle || selectedObject instanceof TestPackage) {
                found = true;
                break;
            }
        }

        if (!found) {
            setErrorMessage("Select 1 or more test classes to run");
            return false;
        }

        return true;
    }

    public List<TestClass> getSelectedClasses() {

        HashMap<String, TestClass> selectedClasses = new HashMap<>();

        ISelection selected = treeViewer.getSelection();
        for (Object selectedObject : ((ITreeSelection) selected).toArray()) {
            if (selectedObject instanceof TestBranch) {
                ((TestBranch) selectedObject).addTests(selectedClasses);
            }
        }

        ArrayList<TestClass> testClasses = new ArrayList<>(selectedClasses.values());
        Collections.sort(testClasses);

        return testClasses;
    }

    public TestStream getTestStream() {
        return this.selectedTestStream;
    }

}
