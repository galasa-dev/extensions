/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.ras.couchdb.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IFrameworkInitialisation;
import dev.galasa.framework.spi.IResultArchiveStoreRegistration;
import dev.galasa.framework.spi.ResultArchiveStoreException;

@Component(service = { IResultArchiveStoreRegistration.class })
public class CouchdbRasRegistration implements IResultArchiveStoreRegistration {

    private IFramework      framework;
    private URI             rasUri;
    private CouchdbRasStore store;

    @Override
    public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
            throws ResultArchiveStoreException {
        this.framework = frameworkInitialisation.getFramework();

        // *** See if this RAS is to be activated, will eventually allow multiples of
        // itself
        final List<URI> rasUris = frameworkInitialisation.getResultArchiveStoreUris();
        for (final URI uri : rasUris) {
            if ("couchdb".equals(uri.getScheme())) {
                if (this.rasUri != null && !store.isShutdown()) {
                    throw new ResultArchiveStoreException(
                            "The CouchDB RAS currently does not support multiple instances of itself");
                }
                this.rasUri = uri;
            }
        }

        if (this.rasUri == null) {
            return;
        }

        // *** Test we can contact the CouchDB server
        try {
            store = new CouchdbRasStore(framework, new URI(this.rasUri.toString().substring(8)));
        } catch (URISyntaxException e) {
            throw new ResultArchiveStoreException("Invalid CouchDB URI " + this.rasUri.getPath());
        }

        // *** All good, register it
        frameworkInitialisation.registerResultArchiveStoreService(store);
    }

}
