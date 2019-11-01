/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import org.eclipse.ui.IWorkbenchPartSite;

public interface IArtifact {

    boolean hasChildren();

    IArtifact[] getChildren();

    IArtifact getChild(String childName);

    String getName();

    void doubleClick(IWorkbenchPartSite iWorkbenchPartSite);
}
