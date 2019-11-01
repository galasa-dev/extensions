/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.Framework;
import dev.galasa.framework.spi.FrameworkException;

public class SubmitTestsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            Framework framework = (Framework) Activator.getInstance().getFramework();

            if (!framework.isInitialised()) {
                MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        "Galasa Framework", "The Galasa Framework is not intialised");
                return null;
            }
        } catch (FrameworkException e) {
            throw new ExecutionException("Unable to determine status of the framework", e);
        }

        try {
            SubmitTestsWizard wizard = new SubmitTestsWizard();
            WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    wizard);

            dialog.open();
        } catch (FrameworkException e) {
            throw new ExecutionException("Unable to open submit tests wizard", e);
        }

        return null;
    }

}
