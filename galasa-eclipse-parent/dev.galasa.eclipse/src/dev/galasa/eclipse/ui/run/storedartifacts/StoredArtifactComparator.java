/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class StoredArtifactComparator extends ViewerComparator {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        return e1.toString().compareTo(e2.toString());
    }

    @Override
    public int category(Object element) {
        return 0;
    }

    @Override
    public boolean isSorterProperty(Object element, String property) {
        return false;
    }

}
