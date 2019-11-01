/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.nio.file.Path;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import dev.galasa.framework.spi.IRunResult;

public class ArtifactEditorInput implements IEditorInput {

    private final IRunResult runResult;
    private final Path       path;

    public ArtifactEditorInput(IRunResult runResult, Path path) {
        this.runResult = runResult;
        this.path = path;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    public Path getPath() {
        return this.path;
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

    @Override
    public String getName() {
        return this.path.getFileName().toString();
    }

}
