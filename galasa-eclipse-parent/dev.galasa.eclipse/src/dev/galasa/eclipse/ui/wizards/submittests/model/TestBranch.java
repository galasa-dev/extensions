/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.HashMap;

public abstract class TestBranch {

    public boolean hasChildren() {
        return false;
    }

    public TestBranch[] getChildren() {
        return new TestBranch[0];
    }

    public void addTests(HashMap<String, TestClass> selectedClasses) {
    }

}
