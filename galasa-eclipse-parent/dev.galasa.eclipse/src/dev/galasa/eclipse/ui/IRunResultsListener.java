/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui;

import java.util.List;

import dev.galasa.framework.spi.IRunResult;

public interface IRunResultsListener {

    void runsUpdate(List<IRunResult> runResults);

}
