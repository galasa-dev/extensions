/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2021.
 */
package dev.galasa.eclipse.ui.run;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.Form;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.run.storedartifacts.ArtifactsPage;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.teststructure.TestStructure;

public class RunEditor extends SharedHeaderFormEditor {

    public final static String ID       = "dev.galasa.eclipse.ui.run.RunEditor";

    private RunEditorInput     editorInput;
    private IRunResult         runResult;
    private TestStructure      testStructure;

    private boolean            disposed = false;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);

        if (!(input instanceof RunEditorInput)) {
            throw new PartInitException("Invalid input to RunEditor (" + input.getClass().getName() + ")");
        }

        this.editorInput = (RunEditorInput) input;
        this.runResult = this.editorInput.getRunResult();
        try {
            this.testStructure = this.runResult.getTestStructure();
        } catch (ResultArchiveStoreException e) {
            throw new PartInitException("Error loading test structure for run " + this.editorInput.getName(), e);
        }

        setTitle("Run " + this.testStructure.getRunName());
    }

    @Override
    protected void addPages() {
        try {
            addPage(new GeneralPage(this, testStructure));
            addPage(new LogPage(this, runResult));
            addPage(new ArtifactsPage(this, runResult));
        } catch (PartInitException e) {
            Activator.log(e);
        }
    }

    @Override
    protected void createHeaderContents(IManagedForm headerForm) {
        super.createHeaderContents(headerForm);

        Form form = headerForm.getForm().getForm();
        form.setText(this.testStructure.getRunName());

        getToolkit().decorateFormHeading(form);

        return;
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
    }

    public boolean isDisposed() {
        return disposed;
    }
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        
        T superAdapter = super.getAdapter(adapter);
        if (superAdapter != null) {
            return superAdapter;
        }
        
        Object selectedPage = getSelectedPage();
        if (selectedPage instanceof IAdaptable) {
            return ((IAdaptable)selectedPage).getAdapter(adapter);
        }

        return null;
    }


}
