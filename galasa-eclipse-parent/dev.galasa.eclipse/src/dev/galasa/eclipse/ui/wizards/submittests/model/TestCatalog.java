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

public class TestCatalog extends TestBranch {

    private final HashMap<String, TestClass> testClasses = new HashMap<>();
    private final TestBundles                testBundles;
    private final TestPackages               testPackages;

    public TestCatalog(JsonObject jsonCatalog) {
        JsonObject classes = jsonCatalog.getAsJsonObject("classes");
        if (classes != null) {
            for (Entry<String, JsonElement> entry : classes.entrySet()) {
                String testId = entry.getKey();
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject) {
                    testClasses.put(testId, new TestClass(testId, (JsonObject) element));
                }
            }
        }

        this.testBundles = new TestBundles(jsonCatalog.getAsJsonObject("bundles"), testClasses);
        this.testPackages = new TestPackages(jsonCatalog.getAsJsonObject("packages"), testClasses);
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public TestBranch[] getChildren() {
        return new TestBranch[] { this.testBundles, this.testPackages };
    }

}
