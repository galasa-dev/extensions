/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;

import dev.galasa.framework.spi.IRunResult;

public class RunEditorInput implements IRunEditorInput {

    private final IRunResult runResult;
    private final String     runName;

    public RunEditorInput(IRunResult runResult, String runName) {
        this.runResult = runResult;
        this.runName = runName;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return this.runName;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> arg0) {
        return null;
    }

    public IRunResult getRunResult() {
        return runResult;
    }

}
