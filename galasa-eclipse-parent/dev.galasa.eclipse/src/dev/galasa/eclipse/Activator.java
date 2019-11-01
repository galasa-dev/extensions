/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import dev.galasa.eclipse.liveupdates.ILiveUpdateServer;
import dev.galasa.eclipse.liveupdates.internal.LiveUpdateServer;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;

public class Activator extends AbstractUIPlugin {

    public static final String                   PLUGIN_ID                = "dev.galasa.eclipse"; //$NON-NLS-1$
    public static final String                   PLUGIN_NAME              = "Galasa";             //$NON-NLS-1$

    private static Activator                     INSTANCE;

    private LinkedList<IFrameworkChangeListener> frameworkChangeListeners = new LinkedList<>();

    private ConsoleLog                           console;

    private Path                                 cachePath;

    private LiveUpdateServer                     liveUpdateServer;

    public Activator() {
        INSTANCE = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        cachePath = Files.createTempDirectory("galasa_eclipse_cache_");
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        deleteCache(cachePath);

        if (liveUpdateServer != null) {
            liveUpdateServer.stop();
        }

        super.stop(context);
    }

    public static Activator getInstance() {
        return INSTANCE;
    }

    public static Path getCachePath() {
        return INSTANCE.cachePath;
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
            for (IFrameworkChangeListener listener : INSTANCE.frameworkChangeListeners) {
                listener.statusChanged(initialised);
            }
        }
    }

    public static void deleteCache(Path path) throws IOException {

        if (path == null) {
            return;
        }

        if (!Files.exists(path)) {
            return;
        }

        if (!Files.isDirectory(path)) {
            Files.delete(path);
            return;
        }

        ArrayList<Path> children = new ArrayList<>();

        Files.list(path).forEach(new Consumer<Path>() {
            @Override
            public void accept(Path subPath) {
                children.add(subPath);
            }
        });

        for (Path child : children) {
            if (Files.isDirectory(child)) {
                deleteCache(child);
            } else {
                Files.deleteIfExists(child);
            }
        }

        Files.delete(path);
    }

    public static ILiveUpdateServer getLiveUpdateServer() throws Exception {
        synchronized (INSTANCE) {
            if (INSTANCE.liveUpdateServer == null) {
                INSTANCE.liveUpdateServer = new LiveUpdateServer();
            }

            return INSTANCE.liveUpdateServer;
        }
    }

}
