/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

import dev.galasa.eclipse.Activator;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.Api;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class TestStream {

    private final String id;
    private final String description;
    private final URI    location;

    public TestStream(String id, IConfigurationPropertyStoreService cps) {
        Objects.nonNull(id);
        Objects.nonNull(cps);

        this.id = id;

        String temp = null;
        try {
            temp = AbstractManager.nulled(cps.getProperty("test.stream." + id, "description"));

            if (temp == null) {
                switch (id) {
                    case "galasa-ivt":
                        temp = "Galasa IVTs";
                        break;
                    case "galasa-int":
                        temp = "Galasa Integration Tests";
                        break;
                    case "simbank":
                        temp = "SimBank Galasa demo tests";
                        break;
                    default:
                        temp = id;
                }
            }
        } catch (ConfigurationPropertyStoreException e) {
            Activator.log(e);
            temp = id;
        }

        this.description = temp;

        // *** Get the location
        URI tempURI = null;
        try {
            String tempLocation = AbstractManager.nulled(cps.getProperty("test.stream." + id, "location"));

            if (tempLocation == null) {
                URL testcatalogUrl = Activator.getInstance().getFramework().getApiUrl(Api.TESTCATALOG);

                tempLocation = testcatalogUrl.toString() + "/" + id;
            }
            tempURI = new URI(tempLocation);
        } catch (Exception e) {
            Activator.log(e);
        }

        this.location = tempURI;

    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public URI getLocation() {
        return this.location;
    }

}
