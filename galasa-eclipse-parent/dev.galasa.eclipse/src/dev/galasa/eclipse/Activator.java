package dev.galasa.eclipse;

import java.util.LinkedList;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;

public class Activator extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "dev.galasa.eclipse"; //$NON-NLS-1$
	public static final String PLUGIN_NAME = "Galasa"; //$NON-NLS-1$
	
	private static Activator INSTANCE;
	
	private LinkedList<IFrameworkChangeListener> frameworkChangeListeners = new LinkedList<>();
	
	private ConsoleLog console;

	public Activator() {
		INSTANCE = this;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	public static Activator getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Log a throwable
	 * 
	 * @param e
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * Log a status
	 * 
	 * @param status
	 */
	public static void log(IStatus status) {
		ILog log = getInstance().getLog();
		if (log != null) {
			log.log(status);
		}
	}

	/**
	 * 
	 * @return - plugin ID
	 */
	public static String getPluginId() {
		return PLUGIN_ID;
	}
	
	
	public IFramework getFramework() throws FrameworkException {
		BundleContext bundleContext = getBundle().getBundleContext();
		ServiceReference<IFramework> frameworkService = bundleContext.getServiceReference(IFramework.class);
		if (frameworkService == null) {
			throw new FrameworkException("The framework service reference is missing");
		}
		IFramework framework = bundleContext.getService(frameworkService);
		if (framework == null) {
			throw new FrameworkException("The framework service is missing");
		}

		return framework;
	}

	public synchronized ConsoleLog getConsole() {
		if (console == null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					console = new ConsoleLog();
				}
			});
		}
		
		return console;
	}
	
	public static void addFrameworkChangeListener(IFrameworkChangeListener listener) {
		synchronized (INSTANCE.frameworkChangeListeners) {
			if (!INSTANCE.frameworkChangeListeners.contains(listener)) {
				INSTANCE.frameworkChangeListeners.add(listener);
			}
		}
	}

	public static void removeFrameworkChangeListener(IFrameworkChangeListener listener) {
		synchronized (INSTANCE.frameworkChangeListeners) {
			INSTANCE.frameworkChangeListeners.remove(listener);
		}
	}
	
	public static void frameworkChange(boolean initialised) {
		synchronized (INSTANCE.frameworkChangeListeners) {
			for(IFrameworkChangeListener listener : INSTANCE.frameworkChangeListeners) {
				listener.statusChanged(initialised);
			}
		}
	}

}
