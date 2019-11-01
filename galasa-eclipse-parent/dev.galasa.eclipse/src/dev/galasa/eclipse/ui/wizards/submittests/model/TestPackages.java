/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TestPackages extends TestBranch {

    private HashMap<String, TestPackage> testPackages = new HashMap<>();

    public TestPackages(JsonObject jsonPackages, HashMap<String, TestClass> testClasses) {
        if (jsonPackages != null) {
            for (Entry<String, JsonElement> entry : jsonPackages.entrySet()) {
                String packageId = entry.getKey();
                JsonElement element = entry.getValue();
                if (element instanceof JsonArray) {
                    testPackages.put(packageId, new TestPackage(packageId, (JsonArray) element, testClasses));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Packages";
    }

    @Override
    public boolean hasChildren() {
        return !testPackages.isEmpty();
    }

    @Override
    public TestBranch[] getChildren() {
        return testPackages.values().toArray(new TestBranch[testPackages.size()]);
    }

    @Override
    public void addTests(HashMap<String, TestClass> selectedClasses) {
        for (TestPackage p : testPackages.values()) {
            p.addTests(selectedClasses);
        }
    }
}
