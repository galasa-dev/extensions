/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

public interface IStoredArtifactsFilter {

    void filter(String runId, IArtifact rootArtifact);

}
