/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.ui.IWorkbenchPartSite;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.IRunResult;

public class ArtifactFile implements IArtifact {

    private final IRunResult runResult;
    private final Path       path;
    private final String     name;

    protected ArtifactFile(IRunResult runResult, Path path) {
        this.runResult = runResult;
        this.path = path;
        this.name = path.getFileName().toString();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public IArtifact[] getChildren() {
        return new IArtifact[0];
    }

    public Path getPath() {
        return path;
    }

    public IRunResult getRunResult() {
        return runResult;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public IArtifact getChild(String childName) {
        return null;
    }

    @Override
    public void doubleClick(IWorkbenchPartSite site) {
        try {
            Map<String, Object> attrs = Files.readAttributes(this.path, "ras:contentType");

            String contentType = (String) attrs.get("ras:contentType");
            if ("plain/text".equals(contentType)) {
                site.getPage().openEditor(new ArtifactEditorInput(this.runResult, this.path), ArtifactEditor.ID);
            }
            if ("image/png".equals(contentType)) {
            	ImageView.openView(this.path);
            }
        } catch (Exception e) {
            Activator.log(e);
        }
    }

}
