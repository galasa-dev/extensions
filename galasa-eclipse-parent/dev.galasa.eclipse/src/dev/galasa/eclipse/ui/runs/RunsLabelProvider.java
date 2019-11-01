/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.runs;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class RunsLabelProvider extends CellLabelProvider {

    @Override
    public void update(ViewerCell cell) {

        String text = cell.getElement().toString();
        cell.setText(text);

    }
}
