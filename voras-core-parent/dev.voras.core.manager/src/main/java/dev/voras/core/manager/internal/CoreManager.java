package dev.voras.core.manager.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import dev.voras.ICredentials;
import dev.voras.ManagerException;
import dev.voras.core.manager.CoreManagerException;
import dev.voras.core.manager.ICoreManager;
import dev.voras.core.manager.Logger;
import dev.voras.core.manager.RunName;
import dev.voras.core.manager.StoredArtifactRoot;
import dev.voras.core.manager.TestProperty;
import dev.voras.framework.spi.AbstractManager;
import dev.voras.framework.spi.ConfigurationPropertyStoreException;
import dev.voras.framework.spi.GenerateAnnotatedField;
import dev.voras.framework.spi.IConfigurationPropertyStoreService;
import dev.voras.framework.spi.IFramework;
import dev.voras.framework.spi.IManager;
import dev.voras.framework.spi.ResourceUnavailableException;
import dev.voras.framework.spi.creds.CredentialsException;

@Component(service = { IManager.class })
public class CoreManager extends AbstractManager implements ICoreManager {

	private IConfigurationPropertyStoreService cpsTest;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.ejat.framework.spi.AbstractManager#initialise(io.ejat.framework.spi.
	 * IFramework, java.util.List, java.util.List, java.lang.Class)
	 */
	@Override
	public void initialise(@NotNull IFramework framework, @NotNull List<IManager> allManagers,
			@NotNull List<IManager> activeManagers, @NotNull Class<?> testClass) throws ManagerException {
		super.initialise(framework, allManagers, activeManagers, testClass);

		try {
			this.cpsTest = framework.getConfigurationPropertyService("test");
		} catch (ConfigurationPropertyStoreException e) {
			throw new CoreManagerException("Unable to initialise the CPS for Core Manager",e);
		}

		// *** We always want the Core Manager initialised and included in the Test Run
		activeManagers.add(this);
	}

	@Override
	public void provisionGenerate() throws ManagerException, ResourceUnavailableException {
		generateAnnotatedFields(CoreManagerField.class);
	}

	/**
	 * Generates a Log instance for the Test Class
	 *
	 * @param field       The field in question
	 * @param annotations All the Manager annotations associated with the field
	 * @return The Object the field needs to be filled with
	 */
	@GenerateAnnotatedField(annotation = Logger.class)
	public Log createLogField(Field field, List<Annotation> annotations) {
		return LogFactory.getLog(getTestClass());
	}

	/**
	 * Generates a ICoreManager instance for the Test Class
	 *
	 * @param field       The field in question
	 * @param annotations All the Manager annotations associated with the field
	 * @return The Object the field needs to be filled with
	 */
	@GenerateAnnotatedField(annotation = dev.voras.core.manager.CoreManager.class)
	public ICoreManager createICoreManager(Field field, List<Annotation> annotations) {
		return this;
	}

	/**
	 * Generates a test Property
	 *
	 * @param field       The field in question
	 * @param annotations All the Manager annotations associated with the field
	 * @return The Object the field needs to be filled with
	 * @throws ConfigurationPropertyStoreException 
	 * @throws CoreManagerException 
	 */
	@GenerateAnnotatedField(annotation = TestProperty.class)
	public String createTestproperty(Field field, List<Annotation> annotations) throws ConfigurationPropertyStoreException, CoreManagerException {

		TestProperty testPropertyAnnotation = field.getAnnotation(TestProperty.class);

		String value = nulled(this.cpsTest.getProperty(testPropertyAnnotation.prefix(), 
				testPropertyAnnotation.suffix(), 
				testPropertyAnnotation.infixes()));

		if (testPropertyAnnotation.required() && value == null) { 
			throw new CoreManagerException("Test Property missing for prefix=" 
					+ testPropertyAnnotation.prefix() 
					+ ",suffix=" 
					+ testPropertyAnnotation.suffix()
					+ ",infixes=" 
					+ testPropertyAnnotation.infixes());
		}

		return value;
	}

	/**
	 * Generates a Stored Artifact Root Path instance for the Test Class
	 *
	 * @param field       The field in question
	 * @param annotations All the Manager annotations associated with the field
	 * @return The Object the field needs to be filled with
	 */
	@GenerateAnnotatedField(annotation = StoredArtifactRoot.class)
	public Path createrootPath(Field field, List<Annotation> annotations) {
		return getFramework().getResultArchiveStore().getStoredArtifactsRoot();
	}

	/**
	 * Generates a Run Name String instance for the Test Class
	 *
	 * @param field       The field in question
	 * @param annotations All the Manager annotations associated with the field
	 * @return The Object the field needs to be filled with
	 */
	@GenerateAnnotatedField(annotation = RunName.class)
	public String createRunName(Field field, List<Annotation> annotations) {
		return getRunName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.ejat.core.manager.ICoreManager#getRunName()
	 */
	@Override
	public @NotNull String getRunName() {
		return getFramework().getTestRunName();
	}

	@Override
	public ICredentials getCredentials(@NotNull String credentialsId) throws CoreManagerException {
		try {
			return getFramework().getCredentialsService().getCredentials(credentialsId);
		} catch (CredentialsException e) {
			throw new CoreManagerException("Unable to retrieve credentials for id " + credentialsId, e);
		}
	}

}
