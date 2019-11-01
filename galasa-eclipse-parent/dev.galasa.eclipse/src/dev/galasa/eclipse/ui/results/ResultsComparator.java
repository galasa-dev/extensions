/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class ResultsComparator extends ViewerComparator {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {

        if (e1 instanceof BranchFolder && e2 instanceof BranchFolder) {
            return ((BranchFolder) e1).getSortOrder() - ((BranchFolder) e2).getSortOrder();
        }

        if (e1 instanceof BranchRun && e2 instanceof BranchRun) {
            return ((BranchRun) e2).getQueued().compareTo(((BranchRun) e1).getQueued());
        }

        return 0;
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
