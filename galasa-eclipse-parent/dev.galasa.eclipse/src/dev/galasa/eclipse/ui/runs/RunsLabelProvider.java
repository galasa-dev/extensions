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
