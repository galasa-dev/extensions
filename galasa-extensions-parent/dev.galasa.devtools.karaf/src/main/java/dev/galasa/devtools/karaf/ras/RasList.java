package dev.galasa.devtools.karaf.ras;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;
import dev.galasa.devtools.karaf.run.RunNameComparator;
import dev.galasa.framework.spi.IResultArchiveStore;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.teststructure.TestStructure;

@Command(scope = "ras", name = "list", description = "List the runs in the RAS (default today's runs only)")
@Service
public class RasList implements Action {

	@Option(name = "-y", aliases = {
	"--yesterday" }, description = "Restrict to yesterdays runs", required = false, multiValued = false)
	private boolean    yesterday;

	@Option(name = "-w", aliases = {
	"--week" }, description = "Restrict to this weeks runs", required = false, multiValued = false)
	private boolean    weeks;

	@Option(name = "-l", aliases = {
	"--lastweek" }, description = "Restrict to last weeks runs", required = false, multiValued = false)
	private boolean    lastweeks;

	@Option(name = "-a", aliases = {
	"--all" }, description = "Do not restrict based on date(not recommended)", required = false, multiValued = false)
	private boolean    all;

	@Option(name = "-r", aliases = {
	"--requestor" }, description = "Restrict runs to a requestor", required = false, multiValued = false)
	private String requestor;


	private final ZoneId zoneId = ZoneId.systemDefault();

	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}

		IResultArchiveStore ras = devEnv.getFramework().getResultArchiveStore();
		List<IResultArchiveStoreDirectoryService> rasDirs = ras.getDirectoryServices();
		
		Instant from = null;
		Instant to   = null;
		
		if (yesterday) {
			ZonedDateTime zdt = ZonedDateTime.now();
			zdt = zdt.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
			from = zdt.toInstant();
			to = zdt.plusDays(1).toInstant();
		} else if (weeks) {
			ZonedDateTime zdt = ZonedDateTime.now();
			zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
			
			DayOfWeek dow = zdt.getDayOfWeek();
			int monday = dow.getValue() - 1;
			zdt = zdt.minusDays(monday);		
			
			from = zdt.toInstant();
		} else if (lastweeks) {
			ZonedDateTime zdt = ZonedDateTime.now();
			zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0).minusWeeks(1);
			
			DayOfWeek dow = zdt.getDayOfWeek();
			int monday = dow.getValue() - 1;
			zdt = zdt.minusDays(monday);		
			
			from = zdt.toInstant();
			to = zdt.plusWeeks(1).toInstant();
		} else if (all) {
		} else {
			ZonedDateTime zdt = ZonedDateTime.now();
			zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
			from = zdt.toInstant();
		}
		
		System.out.println("Listing run with the following criteria:-");
		if (requestor == null) {
			System.out.println("  Requestor = any");
		} else {
			System.out.println("  Requestor = " + requestor);
		}
		if (from == null) {
			System.out.println("  From      = the beginning of time");
		} else {
			System.out.println("  From      = " + convertInstant(from));
		}
		if (to == null) {
			System.out.println("  To        = the end of time");
		} else {
			System.out.println("  To        = " + convertInstant(to));
		}
		
		
		ArrayList<IRunResult> allRuns = new ArrayList<>();
		for(IResultArchiveStoreDirectoryService rasDir : rasDirs) {
			allRuns.addAll(rasDir.getRuns(requestor, from, to));
		}
		
		if (allRuns.isEmpty()) {
			System.err.println("Unable to locate any runs");
			return null;
		}
		
		//*** Sort by runName
		Collections.sort(allRuns, new RunNameComparator());
		
		for(IRunResult result : allRuns) {
			TestStructure ts = result.getTestStructure();
			System.out.println(ts.getRunName() + " - " + ts.getStatus() + " - " + convertInstant(ts.getQueued()));
		}
		
		return null;
	}

	private String convertInstant(Instant time) {
		if (time == null) {
			return "not specified";
		}

		return ZonedDateTime.ofInstant(time, this.zoneId).toString();
	}
}
