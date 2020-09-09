package dev.galasa.eclipse.ui.run.storedartifacts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import dev.galasa.eclipse.Activator;

public class ImageView extends ViewPart implements PaintListener {
	
	 public static final String  ID = "dev.galasa.eclipse.ui.run.storedartifacts.ImageView";

	 private Path                path;
	 private Canvas              canvas;
	 private Composite 			 parent;
	 private Action 			 saveImageToWorkspace;
	 
	 private boolean 			 loaded = false;
	 private Path				 cachedPath;
	 
	 private Image				 image;
	 
	@Override
	public void createPartControl(Composite parent) {
        this.canvas = new Canvas(parent, SWT.NULL);
        this.parent = parent;
        canvas.addPaintListener(this);		
        
        createSaveAction();
        createToolbar();
	}

	@Override
	public void setFocus() {
		this.canvas.setFocus();
		
	}

	@Override
	public void paintControl(PaintEvent event) {
		if (!loaded) {
			displayMessage(event, "Loading Image");
		}
		
		if (this.image == null) {
			displayMessage(event, "Could not display image!");
		}
		
		displayImage(event);

	}
	
	public void displayImage(PaintEvent event) {
		Rectangle viewBounds = parent.getBounds();
		Rectangle bounds = this.image.getBounds();
		event.gc.drawImage(this.image,
				0, 0,
				bounds.width, bounds.height,
				0, 0,
				viewBounds.width, viewBounds.height);
	}
	
	public static void openView(Path path) {
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				try {
					ImageView view = (ImageView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(ImageView.ID, UUID.randomUUID().toString(), IWorkbenchPage.VIEW_ACTIVATE);
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(view);
					view.loadImage(path);
				} catch (PartInitException e) {
					Activator.log(e);
				}
				
			}
		});
		
	}
	
	public void loadImagetoUI() {
		try {
			this.image = new Image(this.canvas.getDisplay(), new ImageData(Files.newInputStream(this.cachedPath)));
		} catch (IOException e) {
			Activator.log(e);
		}
		this.loaded = true;
		redraw();		
	}
	
	public void redraw() {
		Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                ImageView.this.canvas.redraw();
            }
        });
	}
	
	public void loadImage(Path path) {
		new LoadImageJob(this, path).schedule();
	}
	
	public void setCachedImagePath(Path cachedPath) {
		this.cachedPath = cachedPath;
	}
	
	private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        mgr.add(saveImageToWorkspace);
    }
	
	private void createSaveAction() {
		saveImageToWorkspace = new Action("Save Image") {
			public void run() {
				saveImage();						
			}
		};
	}
	
	private void saveImage() {
		FileDialog dlgbox = new FileDialog(this.canvas.getShell(), SWT.SAVE);
	    String[] extensions = { "*.png" };
	    dlgbox.setFilterExtensions(extensions);
	    
	    String filename = dlgbox.open();
	    if (filename != null) {
            if (!filename.endsWith(".png")) {
                filename = filename + ".png";
            }
	    }
	    
	    try {
			Files.copy(this.cachedPath, Paths.get(filename));
		} catch (IOException e) {
			Activator.log(e);
		}
	    
	}
	
	private void displayMessage(PaintEvent event, String message) {
        Rectangle clientArea = this.canvas.getClientArea();

        event.gc.fillRectangle(clientArea);

        event.gc.drawText(message, 10, 10);
    }

}

