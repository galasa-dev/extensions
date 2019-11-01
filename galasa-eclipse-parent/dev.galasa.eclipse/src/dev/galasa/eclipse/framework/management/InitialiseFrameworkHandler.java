/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.framework.management;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;

public class InitialiseFrameworkHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            IFramework framework = Activator.getInstance().getFramework();

            if (framework.isInitialised()) {
                MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        "Galasa Framework", "The Galasa Framework is already initialised");
                return null;
            }

        } catch (FrameworkException e) {
            throw new ExecutionException("Unable to determine status of the framework", e);
        }

        new InitialiseFrameworkJob().schedule();

        return null;
    }

}
