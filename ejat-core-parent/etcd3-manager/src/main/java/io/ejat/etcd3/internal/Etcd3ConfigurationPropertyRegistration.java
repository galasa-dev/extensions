
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import io.etcd.jetcd.ByteSequence;

import org.osgi.service.component.annotations.Component;

import io.ejat.framework.spi.ConfigurationPropertyStoreException;
import io.ejat.framework.spi.IConfigurationPropertyStoreRegistration;
import io.ejat.framework.spi.IFrameworkInitialisation;

@Component(service= {IConfigurationPropertyStoreRegistration.class})
public class Etcd3ConfigurationPropertyRegistration implements IConfigurationPropertyStoreRegistration {

	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws ConfigurationPropertyStoreException {
		System.out.println("here");
	}
}