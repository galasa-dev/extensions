/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.preferences;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class NonEmptyStringFieldEditor extends StringFieldEditor {

    private final String fieldName;

    public NonEmptyStringFieldEditor(String fieldName, String pBootstrapUri, String string,
            Composite fieldEditorParent) {
        super(pBootstrapUri, string, fieldEditorParent);
        this.fieldName = fieldName;
    }

    @Override
    public boolean isValid() {

        String data = getStringValue();

        if (data == null || data.trim().isEmpty()) {
            setErrorMessage(this.fieldName + " is required");
            return false;
        }

        return super.isValid();
    }

}
