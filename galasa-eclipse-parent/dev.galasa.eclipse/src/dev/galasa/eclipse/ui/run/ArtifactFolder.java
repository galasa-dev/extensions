package dev.galasa.eclipse.ui.run;

import java.util.ArrayList;

public class ArtifactFolder implements IArtifact {
	
	private final ArrayList<IArtifact> artifacts = new ArrayList<>();
	private final String name;
	
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
	public Object[] getChildren() {
		return artifacts.toArray();
	}

}
