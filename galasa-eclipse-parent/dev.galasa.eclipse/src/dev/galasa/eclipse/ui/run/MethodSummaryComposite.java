/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import dev.galasa.framework.spi.teststructure.TestMethod;
import dev.galasa.framework.spi.teststructure.TestStructure;

public class MethodSummaryComposite extends Composite {

    private final GeneralPage generalPage;

    private static Font       boldFont;

    public MethodSummaryComposite(FormToolkit toolkit, Composite parent, TestStructure testStructure,
            GeneralPage generalPage) {
        super(parent, SWT.NONE);

        FontData fontData = this.getFont().getFontData()[0];
        boldFont = new Font(Display.getCurrent(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

        this.generalPage = generalPage;

        this.setLayout(new GridLayout(2, false));
        toolkit.paintBordersFor(this);

        toolkit.createLabel(this, "Test Method");
        Label label = toolkit.createLabel(this, "Result");
        label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        List<TestMethod> methods = testStructure.getMethods();
        if (methods == null || methods.isEmpty()) {
            toolkit.adapt(this);
            return;
        }

        for (TestMethod testMethod : methods) {
            processTestMethod(toolkit, testMethod);
        }

        toolkit.adapt(this);
    }

    private void processTestMethod(FormToolkit toolkit, TestMethod testMethod) {
        if (testMethod.getBefores() != null) {
            for (TestMethod before : testMethod.getBefores()) {
                processTestMethod(toolkit, before);
            }
        }

        TestMethodResult result = new TestMethodResult(testMethod, this.generalPage);
        Label name = toolkit.createLabel(this, result.getName());
        Label status = toolkit.createLabel(this, result.getResult());

        if (testMethod.getAfters() != null) {
            for (TestMethod after : testMethod.getAfters()) {
                processTestMethod(toolkit, after);
            }
        }
    }

    private static class TestMethodResult {

        private final String      methodName;
        private final GeneralPage generalPage;
        private String            result;

        public TestMethodResult(TestMethod testMethod, GeneralPage generalPage) {
            this.methodName = testMethod.getMethodName();
            this.result = testMethod.getResult();
            if (this.result == null) {
                this.result = "Unknown";
            }
            this.generalPage = generalPage;
        }

        public String getName() {
            return methodName;
        }

        public String getResult() {
            return result;
        }
    }

}
