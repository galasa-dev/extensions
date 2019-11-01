/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui;

public interface IPropertyListener {

    void propertyUpdate(PropertyUpdate propertyUpdate);

    void propertyUpdateComplete();

}
