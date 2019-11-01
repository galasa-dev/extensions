/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.IPropertyListener;
import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.eclipse.ui.PropertyUpdate;
import dev.galasa.eclipse.ui.PropertyUpdate.Type;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreWatcher;
import dev.galasa.framework.spi.IFramework;

/**
 * maintain a list of the Resources in the ecosystem.
 * 
 * can get slightly out of date during start up due to the watch and job may
 * return slightly out of sync data.
 * 
 * TODO, add housekeeping timer to check contents occasionally
 * 
 * @author Michael Baylis
 *
 */
public class ResourcesParent implements IUIParent, IPropertyListener, IDynamicStatusStoreWatcher {

    // //*** All the displayed runs
    private final ArrayList<ResourceFolder>       folders           = new ArrayList<>();
    private final HashMap<String, Resource>       resourceMap       = new HashMap<>();
    private final HashMap<String, ResourceFolder> folderMap         = new HashMap<>();
    //
    // //*** All pending properties
    private final LinkedList<PropertyUpdate>      pendingProperties = new LinkedList<>();

    private ResourcesView                         view;

    private IDynamicStatusStoreService            dss;
    private UUID                                  watchId;

    // *** Remove old properties that are unlikely to be updated further
    private Timer deletePendingPropertiesTimer;

    protected ResourcesParent(ResourcesView runsView) {
        this.view = runsView;

        // *** Create the delete old invalid properties timer task
        deletePendingPropertiesTimer = new Timer();
        deletePendingPropertiesTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                checkForDeletedPendingProperties();

            }
//		}, 30000, 30000);
        }, 100, 100);

        // *** Go and retrieve all the current resourcess
        new FetchAllResourcesJob(this).schedule();

        // *** Retrieve the DSS and set a watch
        try {
            IFramework framework = Activator.getInstance().getFramework();

            dss = framework.getDynamicStatusStoreService("framework");
            watchId = dss.watchPrefix(this, "resource.");
        } catch (FrameworkException e) {
            Activator.log(e);
        }
    }

    public synchronized void dispose() {
        view = null;

        if (deletePendingPropertiesTimer != null) {
            deletePendingPropertiesTimer.cancel();
            deletePendingPropertiesTimer = null;
        }

        if (dss != null && watchId != null) {
            try {
                dss.unwatch(watchId);
            } catch (DynamicStatusStoreException e) {
                Activator.log(e);
            }
        }

    }

    public void setResourcesView(ResourcesView runsView) {
        this.view = runsView;
    }

    @Override
    public boolean hasChildren() {
        return !folders.isEmpty();
    }

    @Override
    public Object[] getChildren() {
        return folders.toArray();
    }

    @Override
    public String toString() {
        return "Resources";
    }

    /*
     * Called by the fetch all runs job
     */
    @Override
    public synchronized void propertyUpdate(PropertyUpdate propertyUpdate) {

        String[] parts = propertyUpdate.getKey().split("\\.");
        if (parts.length < 4) {
            return;
        }

        if (!"resource".equals(parts[0])) {
            return;
        }

        // *** Resource are based on the .run property prefix, so if it is a .run, find
        // the resource and update/create it
        if (parts[parts.length - 1].equals("run")) {
            // *** Calc the resource name
            String resourceName = getResourceName(parts, 1, parts.length - 2);

            // *** Fetch the resource if it exists
            Resource resource = resourceMap.get(resourceName);
            // *** If delete, then delete it exists
            if (propertyUpdate.getType() == Type.DELETE) {
                if (resource != null) {
                    resourceMap.remove(resourceName);
                    resource.getFolder().remove(resource);
                    update(resource.getFolder());
                }
                return;
            }

            // *** If we do have a resource, simply update it
            if (resource != null) {
                resource.propertyUpdate(propertyUpdate);
                update(resource);
                return;
            }

            // *** We dont have a resource for this new update
            // *** Calculate the folder it should exist in
            String folderName = getResourceName(parts, 1, parts.length - 3);

            // *** Fetch the folder if it exists
            ResourceFolder folder = folderMap.get(folderName);
            if (folder != null) {
                resource = folder.newResource(resourceName, propertyUpdate.getValue());
                resource.extractPendingProperties(pendingProperties);
                resourceMap.put(resourceName, resource);
                update(folder);
                return;
            }

            // *** Need to create the folder tree
            ResourceFolder parentFolder = null;
            String[] folderParts = folderName.split("\\.");
            StringBuilder partFolderName = new StringBuilder();
            for (int i = 0; i < folderParts.length; i++) {
                if (partFolderName.length() > 0) {
                    partFolderName.append(".");
                }
                partFolderName.append(folderParts[i]);

                ResourceFolder nextFolder = folderMap.get(partFolderName.toString());
                if (nextFolder == null) {
                    nextFolder = new ResourceFolder(partFolderName.toString(), parentFolder);
                    folderMap.put(partFolderName.toString(), nextFolder);

                    if (parentFolder == null) {
                        folders.add(nextFolder);
                    }
                }

                parentFolder = nextFolder;
            }

            resource = parentFolder.newResource(resourceName, propertyUpdate.getValue());
            resource.extractPendingProperties(pendingProperties);
            resourceMap.put(resourceName, resource);
            update(this);
            return;
        } else {
            StringBuilder partResourceName = new StringBuilder();
            // *** find the first resource in the tree and update
            for (int i = 1; i < parts.length; i++) {
                if (partResourceName.length() > 0) {
                    partResourceName.append(".");
                }
                partResourceName.append(parts[i]);

                Resource resource = resourceMap.get(partResourceName.toString());
                if (resource != null) {
                    String remainderName = getResourceName(parts, i + 1, parts.length - 1);

                    resource.propertyUpdate(
                            new PropertyUpdate(remainderName, propertyUpdate.getValue(), propertyUpdate.getType()));
                    update(resource);
                    return;
                }
            }

            // *** Didnt find one, put it on the pending queue, if it is not a delete
            if (propertyUpdate.getType() != Type.DELETE) {
                String remainderName = getResourceName(parts, 1, parts.length - 1);
                pendingProperties
                        .add(new PropertyUpdate(remainderName, propertyUpdate.getValue(), propertyUpdate.getType()));
                return;
            }

        }
    }

    private String getResourceName(String[] parts, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (sb.length() > 0) {
                sb.append(".");
            }

            sb.append(parts[i]);
        }
        return sb.toString();
    }

    @Override
    public synchronized void propertyUpdateComplete() {
        if (!folders.isEmpty()) {
            view.expand(this);
        }
    }

    private synchronized void update(Object element) {
        if (view != null) {
            view.refresh(this);
        }
    }

    /*
     * Called by the watch
     * 
     */
    @Override
    public void propertyModified(String key, Event event, String oldValue, String newValue) {
        Type type = null;
        switch (event) {
            case DELETE:
                type = Type.DELETE;
                break;
            case MODIFIED:
            case NEW:
            default:
                type = Type.UPDATE;
                break;
        }

        try {
            propertyUpdate(new PropertyUpdate(key, newValue, type));
        } catch (Throwable t) {
            Activator.log(t);
        }
    }

    /**
     * Check for any invalid runs that can be deleted
     */
    private synchronized void checkForDeletedPendingProperties() {
        Instant fiveSecondsAgo = Instant.now().minusSeconds(10);
        Iterator<PropertyUpdate> iterator = pendingProperties.iterator();
        while (iterator.hasNext()) {
            PropertyUpdate propertyUpdate = iterator.next();

            if (propertyUpdate.getWhen().compareTo(fiveSecondsAgo) < 0) {
                iterator.remove();
            }
        }
    }

}
