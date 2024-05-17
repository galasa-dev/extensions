/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.cps.rest.mocks;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.CertificateStoreException;
import dev.galasa.framework.spi.ConfidentialTextException;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.ICertificateStoreService;
import dev.galasa.framework.spi.IConfidentialTextService;
import dev.galasa.framework.spi.IConfigurationPropertyStore;
import dev.galasa.framework.spi.IDynamicStatusStore;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IFrameworkInitialisation;
import dev.galasa.framework.spi.IResultArchiveStoreService;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.auth.IUserStore;
import dev.galasa.framework.spi.auth.UserStoreException;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.creds.ICredentialsStore;

import java.net.URI;
import java.util.*;

public class MockFrameworkInitialisation implements IFrameworkInitialisation {

    private URI cpsBootstrapUri ;
    private List<IConfigurationPropertyStore> registeredConfigPropertyStores = new ArrayList<IConfigurationPropertyStore>();

    public MockFrameworkInitialisation(URI cpsBootstrapUri) {
        this.cpsBootstrapUri = cpsBootstrapUri;
    }

    @Override
    public @NotNull URI getBootstrapConfigurationPropertyStore() {
        return cpsBootstrapUri;
    }

    @Override
    public URI getDynamicStatusStoreUri() {
        throw new UnsupportedOperationException("Unimplemented method 'getDynamicStatusStoreUri'");
    }

    @Override
    public URI getCredentialsStoreUri() {
        throw new UnsupportedOperationException("Unimplemented method 'getCredentialsStoreUri'");
    }

    @Override
    public @NotNull List<URI> getResultArchiveStoreUris() {
        throw new UnsupportedOperationException("Unimplemented method 'getResultArchiveStoreUris'");
    }


    @Override
    public void registerConfigurationPropertyStore(@NotNull IConfigurationPropertyStore configurationPropertyStore) {
        registeredConfigPropertyStores.add(configurationPropertyStore);
    }

    public List<IConfigurationPropertyStore> getRegisteredConfigurationPropertyStores() {
        return registeredConfigPropertyStores;
    }

    @Override
    public void registerDynamicStatusStore(@NotNull IDynamicStatusStore dynamicStatusStore)
            throws DynamicStatusStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'registerDynamicStatusStore'");
    }

    @Override
    public void registerResultArchiveStoreService(@NotNull IResultArchiveStoreService resultArchiveStoreService)
            throws ResultArchiveStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'registerResultArchiveStoreService'");
    }

    @Override
    public void registerCertificateStoreService(@NotNull ICertificateStoreService certificateStoreService)
            throws CertificateStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'registerCertificateStoreService'");
    }

    @Override
    public void registerConfidentialTextService(@NotNull IConfidentialTextService confidentialTextService)
            throws ConfidentialTextException {
        throw new UnsupportedOperationException("Unimplemented method 'registerConfidentialTextService'");
    }

    @Override
    public void registerCredentialsStore(@NotNull ICredentialsStore credentialsStore) throws CredentialsException {
        throw new UnsupportedOperationException("Unimplemented method 'registerCredentialsStore'");
    }

    @Override
    public @NotNull IFramework getFramework() {
        throw new UnsupportedOperationException("Unimplemented method 'getFramework'");
    }

    @Override
    public void registerUserStore(@NotNull IUserStore userStore) throws UserStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'registerUserStore'");
    }
}
