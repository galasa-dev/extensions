package dev.vorus.core.cps.etcd.internal;

import java.net.URI;
import java.net.URISyntaxException;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import dev.voras.framework.spi.DynamicStatusStoreException;
import dev.voras.framework.spi.IDynamicStatusStoreRegistration;
import dev.voras.framework.spi.IFrameworkInitialisation;

/**
 * This Class is a small OSGI bean that registers the DSS store as a ETCD cluster or quietly fails.
 * 
 * @author James Davies
 */
@Component(service= {IDynamicStatusStoreRegistration.class})
public class Etcd3DynamicStatusStoreRegistration implements IDynamicStatusStoreRegistration {

    /**
     * This intialise method is a overide that registers the correct store to the framework.
     * 
     * The URI is collected from the Intialisation. If the URI is a etcd scheme then it registers it as a etcd.
     * 
     * @param frameworkIntialisation - gives the registrtion access to the correct URI for the dss
     */
	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws DynamicStatusStoreException {
            URI dss = frameworkInitialisation.getDynamicStatusStoreUri();

            if (isEtcdUri(dss)){
                try {
                    URI uri = new URI(dss.toString().replace("etcd:", ""));
                    frameworkInitialisation.registerDynamicStatusStore(new Etcd3DynamicStatusStore(uri));
                } catch (URISyntaxException e) {
                    throw new DynamicStatusStoreException("Could not create URI", e);
                }
            } 
    }
    
    /**
     * A simple check of the scheme to make sure it realtes to a Etcd store
     * @param uri - location of etcd store
     * @return boolean
     */
    public static boolean isEtcdUri(URI uri) {
        return "etcd".equals(uri.getScheme());
    }
}