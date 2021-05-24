/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2021.
 */
package dev.galasa.eclipse.ui.run;

import org.eclipse.jface.text.IFindReplaceTarget;

import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;

import dev.galasa.framework.spi.IRunResult;

public class LogPage extends FormPage {
    public static final String ID = "dev.galasa.eclipse.ui.run.LogPage";

    private final RunEditor    runeditor;
    private final IRunResult   runResult;

    private Composite          theBody;

    private LogComposite       logComposite;

    public LogPage(RunEditor runEditor, IRunResult runResult) {
        super(runEditor, ID, "Run Log");
        this.runeditor = runEditor;
        this.runResult = runResult;
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        Form form = getManagedForm().getForm().getForm();
        FormToolkit toolkit = getEditor().getToolkit();

        form.setText("Run Log");

        theBody = form.getBody();
        GridLayout layout = new GridLayout(1, false);
        theBody.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

        logComposite = new LogComposite(theBody, this.runResult);
        logComposite.setLayoutData(gd);

        toolkit.adapt(logComposite);

        activateActions();

        new FetchLogJob(this, this.runResult).schedule();

        return;
    }

    public void setLog(String log) {
        if (runeditor.isDisposed()) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    setLog(log);
                }
            });
            return;
        }

        // *** Now running on the UI thread
        logComposite.setLog(log);
    }
    
    public void activateActions() { 

        ResourceBundle x = ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages");
        FindReplaceAction fAction = new FindReplaceAction(x, "Editor.FindReplace.", this); //$NON-NLS-1$
        fAction.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_ACTION);
        fAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
        runeditor.getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), fAction);
        fAction.setEnabled(true);
    }

    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        
        if (IFindReplaceTarget.class.equals(adapter)) {
            if (logComposite != null && logComposite.getTextViewer() != null) {
                return (T) logComposite.getTextViewer().getFindReplaceTarget();
            }
        }

        return super.getAdapter(adapter);
    }


}
