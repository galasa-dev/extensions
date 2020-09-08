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
	 
	 private boolean 			 cached = false;
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
		try {
			if (!cached) {
				String fileName = this.path.getFileName().toString();
				cachedPath = Activator.getCachePath().resolve(fileName);
				Files.copy(this.path, cachedPath);
				
				InputStream is = Files.newInputStream(this.cachedPath);
				this.image = new Image(canvas.getDisplay(), new ImageData(is));
			
				cached = true;
			}
			
			
			Rectangle bounds = image.getBounds();
			event.gc.drawImage(image,
					0, 0,
					bounds.width, bounds.height,
					0, 0,
					(int)parent.view.bounds().width, (int)parent.view.bounds().height);
			
		} catch (IOException e) {
			Activator.log(e);
		}
		
		
	}
	
	public static void openView(Path path) {
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				try {
					ImageView view = (ImageView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(ImageView.ID, UUID.randomUUID().toString(), IWorkbenchPage.VIEW_ACTIVATE);
					view.setImagePath(path);
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(view);
				} catch (PartInitException e) {
					Activator.log(e);
				}
				
			}
		});
		
	}
	
	public void setImagePath(Path path) {
		this.path = path;
	}
	
	private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        mgr.add(saveImageToWorkspace);
    }
	
	private void createSaveAction() {
		saveImageToWorkspace = new Action("Save Image to Workspace") {
			public void run() {
				saveImage();						
			}
		};
	}
	
	private void saveImage() {
		String filename = this.path.getFileName().toString();
		
		Path workspace = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString())
				.resolve(filename);
		try {
			Files.copy(this.path, workspace);
		} catch (IOException e) {
			Activator.log(e);
		}
	}

}

