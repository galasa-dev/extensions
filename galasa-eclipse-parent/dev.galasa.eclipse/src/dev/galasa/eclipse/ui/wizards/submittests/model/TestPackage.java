/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests.model;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonArray;

public class TestPackage extends TestBranch {

    private final String               packageId;
    private final ArrayList<TestClass> classes = new ArrayList<>();

    public TestPackage(String packageId, JsonArray jsonPackage, HashMap<String, TestClass> testClasses) {
        this.packageId = packageId;

        if (jsonPackage != null) {
            for (int i = 0; i < jsonPackage.size(); i++) {
                String testClass = jsonPackage.get(i).getAsString();

                TestClass tc = testClasses.get(testClass);
                if (tc != null) {
                    classes.add(tc);
                }
            }
        }
    }

    @Override
    public String toString() {
        return this.packageId;
    }

    @Override
    public boolean hasChildren() {
        return !classes.isEmpty();
    }

    @Override
    public TestBranch[] getChildren() {
        return classes.toArray(new TestBranch[classes.size()]);
    }

    @Override
    public void addTests(HashMap<String, TestClass> selectedClasses) {
        for (TestClass klass : classes) {
            klass.addTests(selectedClasses);
        }
    }

}
