/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import dev.galasa.eclipse.ui.PropertyUpdate;
import dev.galasa.eclipse.ui.PropertyUpdate.Type;

public class Resource {

    private final String                  resourceName;
    private final String                  shortName;
    private final String                  runName;
    private final ResourceFolder          resourceFolder;

    private final HashMap<String, String> properties = new HashMap<>();

    public Resource(String resourceName, String runName, ResourceFolder resourceFolder) {
        this.resourceName = resourceName;
        this.runName = runName;
        this.resourceFolder = resourceFolder;

        String[] parts = this.resourceName.split("\\.");
        this.shortName = parts[parts.length - 1];

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(shortName);
        sb.append(" - ");
        sb.append(runName);
        return sb.toString();
    }

    public synchronized void propertyUpdate(PropertyUpdate propertyUpdate) {
        if (propertyUpdate.getType() == Type.DELETE) {
            properties.remove(propertyUpdate.getKey());
        } else {
            properties.put(propertyUpdate.getKey(), propertyUpdate.getValue());
        }

        return;
    }

    public ResourceFolder getFolder() {
        return this.resourceFolder;
    }

    public void extractPendingProperties(LinkedList<PropertyUpdate> pendingProperties) {
        Iterator<PropertyUpdate> iterator = pendingProperties.iterator();
        while (iterator.hasNext()) {
            PropertyUpdate propertyUpdate = iterator.next();

            String key = propertyUpdate.getKey();

            if (key.startsWith(resourceName)) {
                propertyUpdate(propertyUpdate);
                iterator.remove();
            }
        }
    }

}
