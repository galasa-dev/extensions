package io.ejat.core.etcd3.internal;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import io.ejat.framework.spi.ConfigurationPropertyStoreException;
import io.ejat.framework.spi.IConfigurationPropertyStoreService;
import io.ejat.framework.spi.IFrameworkInitialisation;

@Component(service= {IConfigurationPropertyStoreService.class})
public class Etcd3ConfigurationPropertyStore implements IConfigurationPropertyStoreService {

	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws ConfigurationPropertyStoreException {
		System.out.println("here");
	}

}
