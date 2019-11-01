/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import dev.galasa.eclipse.ui.DateConverter;
import dev.galasa.framework.spi.teststructure.TestStructure;

public class GeneralPage extends FormPage {
    public static final String  ID = "dev.galasa.eclipse.ui.run.GeneralPage";

    private final RunEditor     runEditor;
    private final TestStructure testStructure;

    private Composite           theBody;

    public GeneralPage(RunEditor runEditor, TestStructure testStructure) {
        super(runEditor, ID, "General");
        this.runEditor = runEditor;
        this.testStructure = testStructure;
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        Form form = getManagedForm().getForm().getForm();
        FormToolkit toolkit = getEditor().getToolkit();

        theBody = form.getBody();
        GridLayout layout = new GridLayout(2, true);
        theBody.setLayout(layout);

        theBody.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        Composite leftColumn = toolkit.createComposite(theBody);
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        leftColumn.setLayout(gl);
        leftColumn.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        Composite rightColumn = toolkit.createComposite(theBody);
        rightColumn.setLayout(gl);
        rightColumn.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createOverview(toolkit, leftColumn);
        createMethodSummary(toolkit, rightColumn);
    }

    private void createOverview(FormToolkit toolkit, Composite body) {

        Section overviewSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        overviewSection.setText("Overview");
        overviewSection.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = GridData.FILL;

        Composite overviewComposite = toolkit.createComposite(overviewSection);
        overviewComposite.setLayout(new GridLayout(2, false));
        overviewComposite.setLayoutData(gd);
        toolkit.paintBordersFor(overviewComposite);

        toolkit.createLabel(overviewComposite, "Run Name:");
        Text textRunName = toolkit.createText(overviewComposite, this.testStructure.getRunName(),
                SWT.SINGLE | SWT.READ_ONLY);
        textRunName.setLayoutData(gd);

        toolkit.createLabel(overviewComposite, "Status:");
        Text textStatus = toolkit.createText(overviewComposite, this.testStructure.getStatus(),
                SWT.SINGLE | SWT.READ_ONLY);
        textStatus.setLayoutData(gd);

        toolkit.createLabel(overviewComposite, "Result:");
        Text textResult = toolkit.createText(overviewComposite, this.testStructure.getResult(),
                SWT.SINGLE | SWT.READ_ONLY);
        textResult.setLayoutData(gd);

        toolkit.createLabel(overviewComposite, "Requestor:");
        Text textRequestor = toolkit.createText(overviewComposite, this.testStructure.getRequestor(),
                SWT.SINGLE | SWT.READ_ONLY);
        textRequestor.setLayoutData(gd);

        if (this.testStructure.getBundle() != null) {
            toolkit.createLabel(overviewComposite, "Bundle:");
            Text textBundle = toolkit.createText(overviewComposite, this.testStructure.getBundle(),
                    SWT.SINGLE | SWT.READ_ONLY);
            textBundle.setLayoutData(gd);
        }

        if (this.testStructure.getTestName() != null) {
            toolkit.createLabel(overviewComposite, "Test Name:");
            Text textTestName = toolkit.createText(overviewComposite, this.testStructure.getTestName(),
                    SWT.SINGLE | SWT.READ_ONLY);
            textTestName.setLayoutData(gd);
        }

        if (this.testStructure.getQueued() != null) {
            toolkit.createLabel(overviewComposite, "Queued:");
            Text textQueued = toolkit.createText(overviewComposite,
                    DateConverter.visualDate(this.testStructure.getQueued()), SWT.SINGLE | SWT.READ_ONLY);
            textQueued.setLayoutData(gd);
        }
        if (this.testStructure.getStartTime() != null) {
            toolkit.createLabel(overviewComposite, "Started:");
            Text textQueued = toolkit.createText(overviewComposite,
                    DateConverter.visualDate(this.testStructure.getStartTime()), SWT.SINGLE | SWT.READ_ONLY);
            textQueued.setLayoutData(gd);
        }
        if (this.testStructure.getEndTime() != null) {
            toolkit.createLabel(overviewComposite, "Finished:");
            Text textQueued = toolkit.createText(overviewComposite,
                    DateConverter.visualDate(this.testStructure.getEndTime()), SWT.SINGLE | SWT.READ_ONLY);
            textQueued.setLayoutData(gd);
        }

        overviewSection.setClient(overviewComposite);
    }

    private void createMethodSummary(FormToolkit toolkit, Composite body) {

        Section sectionMethods = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        sectionMethods.setText(this.testStructure.getTestShortName() + " - " + this.testStructure.getResult());
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = GridData.FILL;
        sectionMethods.setLayoutData(gd);

        MethodSummaryComposite compositeMethods = new MethodSummaryComposite(runEditor.getToolkit(), sectionMethods,
                this.testStructure, this);
        sectionMethods.setClient(compositeMethods);
    }

}
