/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.launcher.tabs;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;

public interface IConfigurationGroups {

    void createControl(IConfigurationTab tab, Composite parent);

    void setDefaults(ILaunchConfigurationWorkingCopy config);

    void initializeFrom(ILaunchConfiguration config);

    void performApply(ILaunchConfigurationWorkingCopy configuration);

    boolean validatePage();
}
