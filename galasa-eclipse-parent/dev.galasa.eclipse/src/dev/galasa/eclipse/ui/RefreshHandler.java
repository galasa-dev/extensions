/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class RefreshHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        IStructuredSelection selection = (IStructuredSelection) activePage.getSelection();

        if (selection == null) {
            return null;
        }

        Iterator<?> i = selection.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof IRefreshable) {
                ((IRefreshable) o).refreshCommand();
            }
        }

        return null;
    }

}
