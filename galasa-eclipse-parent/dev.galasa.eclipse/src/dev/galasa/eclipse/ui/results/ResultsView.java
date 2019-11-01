/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.CollapseAllAction;
import dev.galasa.eclipse.ui.ICollapseAllListener;
import dev.galasa.eclipse.ui.IExpandAllListener;
import dev.galasa.eclipse.ui.run.RunEditor;
import dev.galasa.eclipse.ui.run.RunEditorInput;
import dev.galasa.eclipse.ui.runs.RunsContentProvider;
import dev.galasa.eclipse.ui.runs.RunsLabelProvider;

/**
 * Displays a view with all the results in the Galasa ecosystem, both local and
 * remote
 * 
 * TODO add a refresh button
 * 
 * @author Michael Baylis
 *
 */
public class ResultsView extends ViewPart implements ICollapseAllListener, IExpandAllListener {

    public static final String ID       = "dev.galasa.eclipse.ui.results.ResultsView";

    private TreeViewer         viewer;

    private ResultsTreeBase    treeBase = new ResultsTreeBase(this);

    private boolean            disposed = false;

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(2, false));
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

        GridData gridData = new GridData(); // container for treeViewer
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;

        MenuManager contextMenu = new MenuManager();
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
        getSite().setSelectionProvider(viewer);

        viewer.getControl().setLayoutData(gridData);

        viewer.setContentProvider(new RunsContentProvider());
        ColumnViewerToolTipSupport.enableFor(viewer);
        viewer.setLabelProvider(new RunsLabelProvider());
        viewer.setComparator(new ResultsComparator());

        viewer.setAutoExpandLevel(2);
        viewer.setInput(treeBase);

        IToolBarManager toolBarMngr = getViewSite().getActionBars().getToolBarManager();
        toolBarMngr.add(new CollapseAllAction(this));

        viewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                Object selectedNode = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                if (selectedNode == null) {
                    return;
                }

                if (selectedNode instanceof BranchRun) {
                    BranchRun run = (BranchRun) selectedNode;
                    try {
                        getSite().getPage().openEditor(new RunEditorInput(run.getResult(), run.getRunName()),
                                RunEditor.ID);
                    } catch (PartInitException e) {
                        Activator.log(e);
                    }
                } else {
                    boolean expand = !viewer.getExpandedState(selectedNode);
                    viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));

                    if (expand) {
                        if (selectedNode instanceof BranchSelectedRuns) {
                            BranchSelectedRuns bsr = (BranchSelectedRuns) selectedNode;
                            bsr.load();
                        }
                    }
                }
            }
        });

        viewer.addTreeListener(new ITreeViewerListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent arg0) {
                if (arg0.getElement() instanceof BranchSelectedRuns) {
                    BranchSelectedRuns bsr = (BranchSelectedRuns) arg0.getElement();
                    bsr.load();
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent arg0) {
            }
        });

        treeBase.viewCreateFinished();
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();

    }

    @Override
    public void dispose() {
        this.disposed = true;
        this.treeBase.dispose();
        super.dispose();
    }

    public void refresh(Object element) {
        if (disposed || viewer == null) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    refresh(element);
                }
            });
            return;
        }

        // *** Now running on the UI thread

        viewer.refresh(element, true);
    }

    public void expand(Object element) {
        if (disposed || viewer == null) {
            return;
        }

        // *** This method has to run on the UI thread, so switch if required
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    expand(element);
                }
            });
            return;
        }

        // *** Now running on the UI thread

        viewer.expandToLevel(element, 1);
    }

    @Override
    public void expandAll() {
        this.viewer.expandAll();
    }

    @Override
    public void collapseAll() {
        this.viewer.collapseAll();
    }

}
