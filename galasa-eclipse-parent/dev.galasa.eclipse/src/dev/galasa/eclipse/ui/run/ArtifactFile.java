package dev.galasa.eclipse.ui.run;

import java.nio.file.Path;

import dev.galasa.framework.spi.IRunResult;

public class ArtifactFile implements IArtifact {
	
	private final IRunResult runResult;
	private final Path       path;
	private final String     name;
	
	protected ArtifactFile(IRunResult runResult, Path path) {
		this.runResult = runResult;
		this.path      = path;
		this.name      = path.getFileName().toString();
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
	public Object[] getChildren() {
		return new Object[0];
	}
	
	public Path getPath() {
		return path;
	}
	
	public IRunResult getRunResult() {
		return runResult;
	}

}
