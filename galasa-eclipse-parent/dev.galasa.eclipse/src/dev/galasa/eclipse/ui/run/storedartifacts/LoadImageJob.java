package dev.galasa.eclipse.ui.run.storedartifacts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.galasa.eclipse.Activator;

public class LoadImageJob extends Job {
	private ImageView view;
	private Path imagePath;
	
	public LoadImageJob(ImageView view, Path imagePath) {
        super("Load image");

        this.view       = view;
        this.imagePath  = imagePath;
    }
	
	@Override
    protected IStatus run(IProgressMonitor monitor) {
		try {
			String fileName = this.imagePath.getFileName().toString();
			Path cachePath = Activator.getCachePath().resolve(fileName);
			Files.copy(this.imagePath, cachePath);
			
			view.setCachedImagePath(cachePath);
			view.loadImagetoUI();
		} catch (IOException e) {
			return new Status(Status.ERROR, Activator.PLUGIN_ID, "Image failed to Load", e);
		}
		
		return new Status(Status.OK, Activator.PLUGIN_ID, "Image loaded");
	}
}
