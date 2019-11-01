/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import java.util.ArrayList;

import dev.galasa.eclipse.ui.IUIParent;

public class ResourceFolder implements IUIParent {

    private final String                    fullName;
    private final String                    shortName;
    private final ResourceFolder            parent;

    private final ArrayList<Resource>       resources = new ArrayList<>();
    private final ArrayList<ResourceFolder> folders   = new ArrayList<>();

    public ResourceFolder(String folderName, ResourceFolder parentFolder) {
        this.fullName = folderName;
        this.parent = parentFolder;

        String[] parts = folderName.split("\\.");
        shortName = parts[parts.length - 1];

        if (parentFolder != null) {
            parentFolder.addFolder(this);
        }
    }

    private void addFolder(ResourceFolder folder) {
        this.folders.add(folder);
    }

    @Override
    public String toString() {
        return shortName;
    }

    @Override
    public boolean hasChildren() {
        return (!resources.isEmpty() || !folders.isEmpty());
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> children = new ArrayList<>();
        children.addAll(resources);
        children.addAll(folders);
        return children.toArray();
    }

    public void remove(Resource resource) {
        resources.remove(resource);
    }

    public Resource newResource(String resourceName, String value) {
        Resource resource = new Resource(resourceName, value, this);
        resources.add(resource);
        return resource;
    }

}
