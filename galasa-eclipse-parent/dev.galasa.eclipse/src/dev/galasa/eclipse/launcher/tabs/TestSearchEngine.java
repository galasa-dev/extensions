/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.launcher.tabs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;

public class TestSearchEngine extends SearchEngine {

    public static IType[] findTests(IJavaProject javaProject) {

        HashSet<IType> result = new HashSet<IType>();

        try {
            doSearch(javaProject, new ArrayList<String>(), result);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }

        return result.toArray(new IType[result.size()]);
    }

    public static IType[] findTests(IJavaProject javaProject, String packageName) {

        HashSet<IType> result = new HashSet<IType>();

        try {
            doSearch(javaProject, Arrays.asList(new String[] { packageName }), result);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }

        return result.toArray(new IType[result.size()]);
    }

    public static IType[] findTests(IJavaProject javaProject, List<String> packageNames) {

        HashSet<IType> result = new HashSet<IType>();

        try {
            doSearch(javaProject, packageNames, result);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }

        return result.toArray(new IType[result.size()]);
    }

    private static void doSearch(IJavaProject root, List<String> packageNames, HashSet<IType> results)
            throws JavaModelException {

        for (IPackageFragment mypackage : root.getPackageFragments()) {

            if (!packageNames.isEmpty() && !packageNames.contains(mypackage.getElementName())) {
                continue;
            }

            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {

                for (ICompilationUnit unit : mypackage.getCompilationUnits()) {

                    for (IType type : unit.getTypes()) {
                        results.add(type);
                    }
                }
            }
        }
    }

}
