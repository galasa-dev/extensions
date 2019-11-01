/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.run.storedartifacts;

import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import dev.galasa.eclipse.ui.run.RunEditor;
import dev.galasa.framework.spi.IRunResult;

public class ArtifactsPage extends FormPage implements IDoubleClickListener {
    public static final String ID = "dev.galasa.eclipse.ui.run.ArtifactsPage";

    private final RunEditor    runeditor;
    private final IRunResult   runResult;

    private Composite          theBody;

    private TreeViewer         treeStoredArtifacts;

    public ArtifactsPage(RunEditor runEditor, IRunResult runResult) {
        super(runEditor, ID, "Stored Artifacts");
        this.runeditor = runEditor;
        this.runResult = runResult;
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        Form form = getManagedForm().getForm().getForm();
        FormToolkit toolkit = getEditor().getToolkit();

        form.setText("Stored Artifacts");

        theBody = form.getBody();
        GridLayout layout = new GridLayout(1, false);
        theBody.setLayout(layout);

        createStoredArtifacts(toolkit, theBody);

        new FetchStoredArtifactsJob(this, runResult).schedule();

        return;
    }

    private void createStoredArtifacts(FormToolkit toolkit, Composite body) {

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

        Composite storedArtifactsComposite = toolkit.createComposite(body);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        storedArtifactsComposite.setLayoutData(gd);
        storedArtifactsComposite.setLayout(new GridLayout(1, false));
        toolkit.paintBordersFor(storedArtifactsComposite);

        treeStoredArtifacts = new TreeViewer(storedArtifactsComposite, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeStoredArtifacts.getTree().setLayoutData(gd);
        treeStoredArtifacts.setContentProvider(new StoredArtifactContentProvider());
        treeStoredArtifacts.setComparator(new StoredArtifactComparator());
        treeStoredArtifacts.setInput(new TreeLoading());
        treeStoredArtifacts.addDoubleClickListener(this);
        toolkit.adapt(treeStoredArtifacts.getTree());

        MenuManager contextMenu = new MenuManager();
        Menu menu = contextMenu.createContextMenu(treeStoredArtifacts.getControl());
        treeStoredArtifacts.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, treeStoredArtifacts);
        getSite().setSelectionProvider(treeStoredArtifacts);

        return;
    }

    private static class StoredArtifactContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof IArtifact) {
                return ((IArtifact) inputElement).getChildren();
            }
            if (inputElement instanceof TreeLoading) {
                return new Object[] { "Loading..." };
            }
            if (inputElement instanceof TreeEmpty) {
                return new Object[] { "No stored artifacts available at this time" };
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            return getElements(parentElement);
        }

        @Override
        public Object getParent(Object element) {
//			if (element instanceof StoredArtifactPath) {
//				return ((StoredArtifactPath)element).getParent();
//			}
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof IArtifact) {
                return ((IArtifact) element).hasChildren();
            }
            return false;
        }

    }

    private static class TreeLoading {

    }

    private static class TreeEmpty {

    }

    private static class TreeError {

        private final String error;

        public TreeError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return error;
        }

    }

    public void setError(String error) {
        if (runeditor.isDisposed()) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    setError(error);
                }
            });
            return;
        }

        // *** Now running on the UI thread
        treeStoredArtifacts.setInput(new TreeError(error));
    }

    public void setArtifacts(ArtifactFolder rootFolder) {
        if (runeditor.isDisposed()) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    setArtifacts(rootFolder);
                }
            });
            return;
        }

        // *** Now running on the UI thread
        treeStoredArtifacts.setInput(rootFolder);
    }

    @Override
    public void doubleClick(DoubleClickEvent event) {
        if (event.getSource() == treeStoredArtifacts) {
            if (event.getSelection() instanceof TreeSelection) {
                TreeSelection selection = (TreeSelection) event.getSelection();

                List<?> selectedList = selection.toList();
                for (Object selected : selectedList) {
                    if (selected instanceof ArtifactFolder) {
                        treeStoredArtifacts.expandToLevel(selected, 1);
                    } else if (selected instanceof IArtifact) {
                        ((IArtifact) selected).doubleClick(getSite());
                    }
                }
            }
        }
    }
}
