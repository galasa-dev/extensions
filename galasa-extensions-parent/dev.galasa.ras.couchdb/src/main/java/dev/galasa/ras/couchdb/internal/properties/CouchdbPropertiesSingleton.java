package dev.galasa.ras.couchdb.internal.properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.ras.couchdb.internal.CouchdbRasException;

@Component(service=CouchdbPropertiesSingleton.class, immediate=true)
public class CouchdbPropertiesSingleton {

    private static CouchdbPropertiesSingleton singletonInstance;

    private static void setInstance(CouchdbPropertiesSingleton instance) {
        singletonInstance = instance;
    }
    
    private IConfigurationPropertyStoreService cps;
    
    @Activate
    public void activate() {
        setInstance(this);
    }
    
    @Deactivate
    public void deacivate() {
        setInstance(null);
    }
    
    public static IConfigurationPropertyStoreService cps() throws CouchdbRasException {
        if (singletonInstance != null) {
            return singletonInstance.cps;
        }

        throw new CouchdbRasException("Attempt to access CPS before it has been initialised");
    }
    
    public static void setCps(IConfigurationPropertyStoreService cps) throws CouchdbRasException {
        if (singletonInstance != null) {
            singletonInstance.cps = cps;
            return;
        }
    
        throw new CouchdbRasException("Attempt to set CPS before instance created");
    }
    
}
