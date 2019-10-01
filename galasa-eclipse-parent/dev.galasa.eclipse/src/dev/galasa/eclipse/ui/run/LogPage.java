package dev.galasa.eclipse.ui.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import dev.galasa.framework.spi.IRunResult;

public class LogPage extends FormPage {
	public static final String ID = "dev.galasa.eclipse.ui.run.LogPage";
	
	private final RunEditor     runeditor;
	private final IRunResult    runResult;
	
	private Composite theBody;

	private LogComposite logComposite;
	
	public LogPage(RunEditor runEditor, IRunResult runResult) {
		super(runEditor, ID, "Run Log");
		this.runeditor     = runEditor;
		this.runResult     = runResult;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		Form form = getManagedForm().getForm().getForm();
		FormToolkit  toolkit = getEditor().getToolkit();
		
		form.setText("Run Log");

		theBody = form.getBody();
		GridLayout layout = new GridLayout(1, false);
		theBody.setLayout(layout);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);

		logComposite = new LogComposite(theBody, this.runResult);
		logComposite.setLayoutData(gd);
		
		toolkit.adapt(logComposite);
		
		new FetchLogJob(this, this.runResult).schedule();
		
		return;
	}

	public void setLog(String log) {
		if (runeditor.isDisposed()) {
			return;
		}
		
		//*** This method has to run on the UI thread, so switch if required
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					setLog(log);
				}
			});
			return;
		}

		//*** Now running on the UI thread
		logComposite.setLog(log);
	}
	
	
}
