/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.resources;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import dev.galasa.eclipse.ui.IUIParent;

public class ResourcesContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    @Override
    public Object[] getChildren(Object parentElement) {

        if (parentElement instanceof IUIParent) {
            return ((IUIParent) parentElement).getChildren();
        }

        return new Object[0];
    }

    @Override
    public boolean hasChildren(Object parentElement) {
        if (parentElement instanceof IUIParent) {
            return ((IUIParent) parentElement).hasChildren();
        }

        return false;
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

}
