package dev.galasa.eclipse.ui.resources;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class ResourcesLabelProvider extends CellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		
		String text = cell.getElement().toString();
		cell.setText(text);

	}
}
