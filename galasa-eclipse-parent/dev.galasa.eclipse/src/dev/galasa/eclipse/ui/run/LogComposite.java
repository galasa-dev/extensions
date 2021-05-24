/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2021.
 */
package dev.galasa.eclipse.ui.run;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import dev.galasa.framework.spi.IRunResult;

public class LogComposite extends Composite {

    private final IRunResult runResult;

    private TextViewer       textViewer;

    private Document         doc;

    public LogComposite(Composite parent, IRunResult runResult) {
        super(parent, SWT.BORDER);
        this.runResult = runResult;

        setLayout(new GridLayout(1, true));

        Composite lvComposite = new Composite(this, SWT.NONE);
        lvComposite.setLayout(new FillLayout());

        textViewer = new TextViewer(lvComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 0;
        gd.widthHint = 0;

        lvComposite.setLayoutData(gd);

        textViewer.setEditable(false);
        doc = new Document();
        textViewer.setDocument(doc);
        doc.set("Loading, please wait...");

        textViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));

    }

    @Override
    public boolean setFocus() {
        return textViewer.getControl().setFocus();
    }

    public void setLog(String log) {
        this.doc.set(log);
    }
    
    public TextViewer getTextViewer() {
        return textViewer;
    }

}
