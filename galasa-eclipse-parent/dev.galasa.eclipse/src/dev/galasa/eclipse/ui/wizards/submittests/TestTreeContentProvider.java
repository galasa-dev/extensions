/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.submittests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import dev.galasa.eclipse.ui.wizards.submittests.model.TestBranch;

/**
 * Provides the content of the Test Selection results tree nodes
 * 
 * @author Michael Baylis
 *
 */
public class TestTreeContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    Pattern      pattern         = Pattern.compile("");
    List<String> selectedClasses = new ArrayList<String>();

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        return;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public Object[] getChildren(Object parentElement) {

        if (parentElement instanceof TestBranch) {
            return ((TestBranch) parentElement).getChildren();
        }

        return new Object[0];
    }

//	private boolean findMatchingTestClasses(TestCollection tc){
//		
//		if (tc.testClasses != null){
//			for (TestClass t: tc.testClasses){
//				if (pattern.matcher(t.classId).find()){
//					return true;
//				}
//				if (selectedClasses.contains(t.classId)){
//					return true;
//				}
//			}
//		}
//		
//		if (tc.collections != null){
//			for (TestCollection searching: tc.collections){
//				if (findMatchingTestClasses(searching)){
//					return true;
//				}
//			}
//		}
//		
//		return false;
//		
//	}

//	public void updateSearchText(String newSearchText, List<String> selectedClasses){
//		pattern = Pattern.compile(newSearchText, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
//		this.selectedClasses = selectedClasses;
//	}

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof TestBranch) {
            return ((TestBranch) element).hasChildren();
        }

        return false;
    }

    public void dispose() {
    }

}
