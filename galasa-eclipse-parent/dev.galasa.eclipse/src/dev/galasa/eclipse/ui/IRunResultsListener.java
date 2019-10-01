package dev.galasa.eclipse.ui;

import java.util.List;

import dev.galasa.framework.spi.IRunResult;

public interface IRunResultsListener {
	
	void runsUpdate(List<IRunResult> runResults);

}
