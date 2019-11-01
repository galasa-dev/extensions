/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.HashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TestClass extends TestBranch implements Comparable<TestClass> {

    private final String testId;
    private final String name;
    private final String bundle;
    private final String shortName;
    private final String packageName;
    private final String summary;

    public TestClass(String testId, JsonObject jsonClass) {
        this.testId = testId;
        this.name = getString(jsonClass, "name");
        this.bundle = getString(jsonClass, "bundle");
        this.shortName = getString(jsonClass, "shortName");
        this.packageName = getString(jsonClass, "package");
        this.summary = getString(jsonClass, "summary");
    }

    private String getString(JsonObject json, String property) {
        JsonElement element = json.get(property);
        if (element == null) {
            return null;
        }

        return element.getAsString();
    }

    @Override
    public String toString() {
        return this.shortName;
    }

    public String getShortName() {
        return this.shortName;
    }

    public String getSummary() {
        return summary;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getBundle() {
        return bundle;
    }

    public String getId() {
        return testId;
    }

    @Override
    public int compareTo(TestClass o) {
        return this.testId.compareTo(o.testId);
    }

    @Override
    public void addTests(HashMap<String, TestClass> selectedClasses) {
        if (!selectedClasses.containsKey(testId)) {
            selectedClasses.put(testId, this);
        }
    }

}
