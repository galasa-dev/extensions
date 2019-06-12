package dev.voras.core.manager.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import dev.voras.ManagerException;
import dev.voras.core.manager.ICoreManager;
import dev.voras.core.manager.Logger;
import dev.voras.core.manager.RunName;
import dev.voras.core.manager.StoredArtifactRoot;
import dev.voras.framework.spi.AbstractManager;
import dev.voras.framework.spi.GenerateAnnotatedField;
import dev.voras.framework.spi.IFramework;
import dev.voras.framework.spi.IManager;
import dev.voras.framework.spi.ResourceUnavailableException;

@Component(service = { IManager.class })
public class CoreManager extends AbstractManager implements ICoreManager {

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

    /*
     * (non-Javadoc)
     * 
     * @see io.ejat.core.manager.ICoreManager#getRunName()
     */
    @Override
    public @NotNull String getRunName() {
        return getFramework().getTestRunName();
    }

}
