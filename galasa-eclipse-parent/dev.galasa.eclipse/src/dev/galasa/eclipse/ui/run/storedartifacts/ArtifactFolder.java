/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.util.ArrayList;

import org.eclipse.ui.IWorkbenchPartSite;

public class ArtifactFolder implements IArtifact {

    private final ArrayList<IArtifact> artifacts = new ArrayList<>();
    private final String               name;

    protected ArtifactFolder(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void addArtifact(IArtifact artifact) {
        this.artifacts.add(artifact);
    }

    @Override
    public boolean hasChildren() {
        return !artifacts.isEmpty();
    }

    @Override
    public IArtifact[] getChildren() {
        return artifacts.toArray(new IArtifact[artifacts.size()]);
    }

    public IArtifact getChild(String childName) {
        for (IArtifact artifact : artifacts) {
            if (childName.equals(artifact.getName())) {
                return artifact;
            }
        }

        return null;
    }

    public String getName() {
        return this.name;
    }

    public void replaceArtifact(IArtifact oldArtifact, IArtifact newArtifact) {
        for (int i = 0; i < artifacts.size(); i++) {
            if (artifacts.get(i) == oldArtifact) {
                artifacts.remove(i);
                artifacts.add(i, newArtifact);
                return;
            }
        }

    }

    @Override
    public void doubleClick(IWorkbenchPartSite site) {
    }

}
