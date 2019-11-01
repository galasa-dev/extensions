/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.HashMap;

import com.google.gson.JsonObject;

public class TestBundle extends TestBranch {

    private final String       bundleId;
    private final TestPackages testPackages;

    public TestBundle(String bundleId, JsonObject jsonBundle, HashMap<String, TestClass> testClasses) {
        this.bundleId = bundleId;
        testPackages = new TestPackages(jsonBundle.getAsJsonObject("packages"), testClasses);
    }

    @Override
    public String toString() {
        return this.bundleId;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public TestBranch[] getChildren() {
        return new TestBranch[] { this.testPackages };
    }

    @Override
    public void addTests(HashMap<String, TestClass> selectedClasses) {
        testPackages.addTests(selectedClasses);
    }

}
