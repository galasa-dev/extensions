/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.launcher;

import java.util.Properties;

import org.eclipse.debug.core.ILaunchConfiguration;

public interface ILauncherOverridesExtension {

    void appendOverrides(ILaunchConfiguration configuration, Properties generatedOverrides);

}
