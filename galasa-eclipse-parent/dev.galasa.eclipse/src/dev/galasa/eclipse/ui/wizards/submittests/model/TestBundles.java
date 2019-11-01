/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TestBundles extends TestBranch {

    private HashMap<String, TestBundle> testBundles = new HashMap<>();

    public TestBundles(JsonObject jsonBundles, HashMap<String, TestClass> testClasses) {
        if (jsonBundles != null) {
            for (Entry<String, JsonElement> entry : jsonBundles.entrySet()) {
                String bundleId = entry.getKey();
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject) {
                    testBundles.put(bundleId, new TestBundle(bundleId, (JsonObject) element, testClasses));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Bundles";
    }

    @Override
    public boolean hasChildren() {
        return !testBundles.isEmpty();
    }

    @Override
    public TestBranch[] getChildren() {
        return testBundles.values().toArray(new TestBranch[testBundles.size()]);
    }

}
