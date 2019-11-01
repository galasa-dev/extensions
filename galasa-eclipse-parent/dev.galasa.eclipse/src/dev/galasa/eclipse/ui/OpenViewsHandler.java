/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.results.ResultsView;
import dev.galasa.eclipse.ui.runs.RunsView;

public class OpenViewsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ResultsView.ID);
        } catch (PartInitException e) {
            Activator.log(e);
        }
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(RunsView.ID);
        } catch (PartInitException e) {
            Activator.log(e);
        }

        return null;
    }

}
