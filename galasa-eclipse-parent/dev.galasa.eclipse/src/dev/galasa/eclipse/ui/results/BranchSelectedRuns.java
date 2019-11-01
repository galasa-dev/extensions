/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.results;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.ui.IRefreshable;
import dev.galasa.eclipse.ui.IRunResultsListener;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;

public class BranchSelectedRuns extends BranchFolder implements IRunResultsListener, IRefreshable {

    private boolean loaded = false;

    public enum DateRange {
        TODAY,
        YESTERDAY,
        EARLIER_THIS_WEEK,
        LAST_WEEK,
        OLDER
    }

    private final IResultArchiveStoreDirectoryService dirService;
    private final String                              folderName;
    private final String                              requestor;
    private final String                              testClass;
    private final DateRange                           dateRange;

    protected BranchSelectedRuns(ResultsView view, IResultArchiveStoreDirectoryService dirService, String folderName,
            String requestor, String testClass, DateRange dateRange) {
        super(view, dirService, Icon.none, dateRange.ordinal());

        this.dirService = dirService;
        this.folderName = folderName;
        this.requestor = requestor;
        this.testClass = testClass;
        this.dateRange = dateRange;
    }

    @Override
    public String toString() {
        return this.folderName;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void refresh() {
        loaded = false;
        setLoading(true);
        branches.clear();
        getView().refresh(this);

        Instant from = null;
        Instant to = null;

        ZonedDateTime zdt = ZonedDateTime.now();
        switch (dateRange) {
            case EARLIER_THIS_WEEK:
                zdt = zdt.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                to = zdt.toInstant();

                DayOfWeek dow = zdt.getDayOfWeek();
                int monday = dow.getValue() - 1;
                zdt = zdt.minusDays(monday);
                from = zdt.toInstant();
                break;
            case LAST_WEEK:
                zdt = zdt.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
                dow = zdt.getDayOfWeek();
                monday = dow.getValue() - 1;
                zdt = zdt.minusDays(monday);
                from = zdt.toInstant();
                to = zdt.plusDays(7).toInstant();
                break;
            case OLDER:
                zdt = zdt.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
                dow = zdt.getDayOfWeek();
                monday = dow.getValue() - 1;
                zdt = zdt.minusDays(monday);
                to = zdt.toInstant();
                break;
            case YESTERDAY:
                zdt = zdt.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                from = zdt.toInstant();
                to = zdt.plusDays(1).toInstant();
                break;
            case TODAY:
            default:
                zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
                from = zdt.toInstant();
                break;
        }

        new FetchRunsJob(requestor, testClass, from, to, dirService, this).schedule();
    }

    @Override
    public void runsUpdate(List<IRunResult> runResults) {
        setLoading(false);
        synchronized (branches) {
            branches.clear();
            for (IRunResult runResult : runResults) {
                try {
                    branches.add(new BranchRun(getView(), dirService, runResult));

                } catch (Exception e) {
                    Activator.log(e);
                }
            }
        }
        loaded = true;
        getView().refresh(this);
    }

    public void load() {
        if (!loaded) {
            setLoading(true);
            refresh();
            getView().refresh(this);
        }
    }

    @Override
    public void refreshCommand() {
        refresh();
    }

}
