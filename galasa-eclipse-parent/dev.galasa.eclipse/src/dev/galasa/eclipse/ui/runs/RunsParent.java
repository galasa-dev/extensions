/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.runs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.IPropertyListener;
import dev.galasa.eclipse.ui.IUIParent;
import dev.galasa.eclipse.ui.PropertyUpdate;
import dev.galasa.eclipse.ui.PropertyUpdate.Type;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreWatcher;
import dev.galasa.framework.spi.IFramework;

/**
 * maintain a list of the runs in the ecosystem.
 * 
 * can get slightly out of date during start up due to the watch and job may
 * return slightly out of sync data.
 * 
 * TODO, add housekeeping timer to check contents occasionally
 * 
 * @author Michael Baylis
 *
 */
public class RunsParent implements IUIParent, IPropertyListener, IDynamicStatusStoreWatcher {

    // *** All the displayed runs
    private final ArrayList<Run>       runs   = new ArrayList<>();
    private final HashMap<String, Run> runMap = new HashMap<>();

    // *** All the runs received, but are not yet valid
    private final HashMap<String, Run> pendingRunMap = new HashMap<>();

    private RunsView                   view;

    private IDynamicStatusStoreService dss;
    private UUID                       watchId;

    // *** Remove old invalid runs that are unlikely to be updated further
    private Timer deletePendingRunsTimer;

    protected RunsParent(RunsView runsView) {
        this.view = runsView;

        // *** Create the delete old invalid runs timer task
        deletePendingRunsTimer = new Timer();
        deletePendingRunsTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                checkForDeletedPendingRuns();

            }
        }, 30000, 30000);

        // *** Go and retrieve all the current runs
        new FetchAllRunsJob(this).schedule();

        // *** Retrieve the DSS and set a watch
        try {
            IFramework framework = Activator.getInstance().getFramework();

            dss = framework.getDynamicStatusStoreService("framework");
            watchId = dss.watchPrefix(this, "run.");
        } catch (FrameworkException e) {
            Activator.log(e);
        }
    }

    public synchronized void dispose() {
        view = null;

        if (deletePendingRunsTimer != null) {
            deletePendingRunsTimer.cancel();
            deletePendingRunsTimer = null;
        }

        if (dss != null && watchId != null) {
            try {
                dss.unwatch(watchId);
            } catch (DynamicStatusStoreException e) {
                Activator.log(e);
            }
        }

    }

    public void setRunsView(RunsView runsView) {
        this.view = runsView;
    }

    @Override
    public boolean hasChildren() {
        return !runs.isEmpty();
    }

    @Override
    public Object[] getChildren() {
        return runs.toArray();
    }

    @Override
    public String toString() {
        return "Runs";
    }

    /*
     * Called by the fetch all runs job
     */
    @Override
    public synchronized void propertyUpdate(PropertyUpdate propertyUpdate) {
        String key = propertyUpdate.getKey();
        String value = propertyUpdate.getValue();
        Type type = propertyUpdate.getType();
        String[] parts = key.split("\\.");
        if (parts.length < 3) {
            return;
        }

        if (!"run".equals(parts[0])) {
            return;
        }

        String runName = parts[1];

        key = key.substring(4 + runName.length() + 1);

        Run run = runMap.get(runName);
        if (run == null) {
            if (value != null) {
                run = pendingRunMap.get(runName);
                if (run != null) {
                    run.propertyUpdate(key, value, type);
                    if (run.isValid()) {
                        runs.add(run);
                        runMap.put(runName, run);
                        pendingRunMap.remove(runName);
                        update(this);
                    }
                } else {
                    run = new Run(runName);
                    run.propertyUpdate(key, value, type);
                    if (run.isValid()) {
                        runs.add(run);
                        runMap.put(runName, run);
                        update(this);
                    } else {
                        pendingRunMap.put(runName, run);
                    }
                }
            }
        } else {
            run.propertyUpdate(key, value, type);
            if (run.isValid()) {
                update(run);
            } else {
                runs.remove(run);
                runMap.remove(run.getRunName());
                update(this);
            }
        }
    }

    @Override
    public synchronized void propertyUpdateComplete() {
        if (!runs.isEmpty()) {
            if (view != null) {
                view.expand(this);
            }
        }
    }

    private synchronized void update(Object element) {
        if (view != null) {
            view.refresh(this);
        }
    }

    /*
     * Called by the watch
     * 
     */
    @Override
    public void propertyModified(String key, Event event, String oldValue, String newValue) {
        Type type = null;
        switch (event) {
            case DELETE:
                type = Type.DELETE;
                break;
            case MODIFIED:
            case NEW:
            default:
                type = Type.UPDATE;
                break;
        }

        try {
            propertyUpdate(new PropertyUpdate(key, newValue, type));
        } catch (Throwable t) {
            Activator.log(t);
        }
    }

    /**
     * Check for any invalid runs that can be deleted
     */
    private synchronized void checkForDeletedPendingRuns() {
        ArrayList<Run> pendingRuns = new ArrayList<>(pendingRunMap.values());
        for (Run pendingRun : pendingRuns) {
            if (!pendingRun.updatedRecently() && !pendingRun.isValid()) {
                pendingRunMap.remove(pendingRun.getRunName());
            }
        }
    }

}
