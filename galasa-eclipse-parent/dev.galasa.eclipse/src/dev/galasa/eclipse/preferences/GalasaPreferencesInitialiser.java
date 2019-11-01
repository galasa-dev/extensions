/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.preferences;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import dev.galasa.eclipse.Activator;

public class GalasaPreferencesInitialiser extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {

        IPreferenceStore store = Activator.getInstance().getPreferenceStore();

        Path galasaDir = Paths.get(System.getProperty("user.home"), ".galasa");

        String username = System.getProperty("user.name");
        if (username == null || username.trim().isEmpty()) {
            username = "unknown";
        }

        store.setDefault(PreferenceConstants.P_BOOTSTRAP_URI,
                galasaDir.resolve("bootstrap.properties").toUri().toString());
        store.setDefault(PreferenceConstants.P_OVERRIDES_URI,
                galasaDir.resolve("overrides.properties").toUri().toString());
        store.setDefault(PreferenceConstants.P_REQUESTOR_ID, username);
        store.setDefault(PreferenceConstants.P_REMOTEMAVEN_URI, "https://repo.maven.apache.org/maven2/");
    }

}
