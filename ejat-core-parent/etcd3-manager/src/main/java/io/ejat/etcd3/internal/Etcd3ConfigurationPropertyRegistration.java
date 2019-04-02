package io.ejat.etcd3.internal;

import java.net.URI;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import io.ejat.framework.spi.ConfigurationPropertyStoreException;
import io.ejat.framework.spi.IConfigurationPropertyStoreRegistration;
import io.ejat.framework.spi.IFrameworkInitialisation;

@Component(service= {IConfigurationPropertyStoreRegistration.class})
public class Etcd3ConfigurationPropertyRegistration implements IConfigurationPropertyStoreRegistration {

	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws ConfigurationPropertyStoreException {
            URI cps = frameworkInitialisation.getBootstrapConfigurationPropertyStore();

            if (isHttpUri(cps)){
                frameworkInitialisation.registerConfigurationPropertyStore(new Etcd3ConfigurationPropertyStore(cps));
            } 
    }
    
    public static boolean isHttpUri(URI uri) {
        return "http".equals(uri.getScheme());
    }
}
