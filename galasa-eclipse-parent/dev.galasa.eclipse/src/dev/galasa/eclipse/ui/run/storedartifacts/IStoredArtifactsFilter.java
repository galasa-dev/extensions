package dev.galasa.eclipse.ui.run.storedartifacts;

public interface IStoredArtifactsFilter {
    
    void filter(String runId, IArtifact rootArtifact);

}
