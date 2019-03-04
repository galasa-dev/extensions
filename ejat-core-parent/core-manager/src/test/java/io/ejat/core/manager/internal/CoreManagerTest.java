package io.ejat.core.manager.internal;

import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.ejat.core.manager.ICoreManager;
import io.ejat.core.manager.Logger;
import io.ejat.core.manager.RunName;
import io.ejat.core.manager.StoredArtifactRoot;
import io.ejat.framework.spi.IFramework;
import io.ejat.framework.spi.IManager;
import io.ejat.framework.spi.IResultArchiveStore;
import io.ejat.framework.spi.ManagerException;
import io.ejat.framework.spi.ResourceUnavailableException;

@RunWith(MockitoJUnitRunner.class)
public class CoreManagerTest {
    
    @Mock
    private IFramework framework;
    
    @Mock
    private IResultArchiveStore ras;
    
    private String runName = UUID.randomUUID().toString();
    
    @Before
    public void setup() {
        when(framework.getTestRunName()).thenReturn(runName);
        when(framework.getResultArchiveStore()).thenReturn(ras);
        when(ras.getStoredArtifactsRoot()).thenReturn(Paths.get("/a/root/dir"));
    }
    
    @Test
    public void testCoreManagerInitialise() throws ManagerException {
        CoreManager coreManager = new CoreManager();
        
        ArrayList<IManager> activeManagers = new ArrayList<>();
        coreManager.initialise(framework, null, activeManagers, null);
        
        Assert.assertTrue("Core Manager missing from active managers", activeManagers.contains(coreManager));
    }

    @Test
    public void testCoreManagerGenerate() throws ManagerException, ResourceUnavailableException {
        CoreManager coreManager = new CoreManager();
        TestClass testClass = new TestClass();
        
        ArrayList<IManager> activeManagers = new ArrayList<>();
        coreManager.initialise(framework, null, activeManagers, testClass.getClass());

        coreManager.provisionGenerate();
        coreManager.fillAnnotatedFields(testClass);
        
        Assert.assertNotNull("Core Manager field missing", testClass.coreManager);
        Assert.assertNotNull("Logger field missing", testClass.logger);
        Assert.assertNotNull("Run Name field missing", testClass.runName);
        Assert.assertNotNull("Root field missing", testClass.root);
        
        Assert.assertEquals("Core Manager field not valid", coreManager, testClass.coreManager);
        Assert.assertTrue("Logger field not valid", (testClass.logger instanceof Log));
        Assert.assertEquals("Core Manager field not valid", this.runName, testClass.runName);
        Assert.assertEquals("Logger field not valid", "/a/root/dir", testClass.root.toString());
    }
    
    
    public static class TestClass {
        @io.ejat.core.manager.CoreManager
        public ICoreManager coreManager;
        
        @Logger
        public Log logger;
        
        @RunName
        public String runName;
        
        @StoredArtifactRoot
        public Path root;
    }
    

}
