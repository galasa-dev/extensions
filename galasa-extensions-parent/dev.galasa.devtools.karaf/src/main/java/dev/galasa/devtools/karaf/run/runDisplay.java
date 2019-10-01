package dev.galasa.devtools.karaf.run;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import dev.galasa.devtools.karaf.DevEnvironment;

@Command(scope = "run", name = "display", description = "Display information about a run")
@Service
public class runDisplay implements Action {

	@Argument(index = 0, name = "runName", description = "The run to display", required = true)
	private String    runName;
	
	private final Pattern patternDateTime = Pattern.compile("\\d\\d\\d\\d\\-\\d\\d\\-\\d\\dT\\d\\d\\:\\d\\d\\:\\d\\d\\.\\d\\d\\dZ");
	
	private final DateTimeFormatter dtf = DateTimeFormatter.RFC_1123_DATE_TIME;

	@Override
	public Object execute() throws Exception {

		final DevEnvironment devEnv = DevEnvironment.getDevEnvironment();

		if (!devEnv.isFrameworkInitialised()) {
			System.err.println("The Framework has not been initialised, use cirillo:init");
			return null;
		}
		runName = runName.toUpperCase();
		
		String prefix = "run." + runName + ".";

		Map<String, String> properties = devEnv.getFramework().getDynamicStatusStoreService("framework").getPrefix("run." + runName);
		
		if (properties.isEmpty()) {
			System.err.println("Run " + runName + " not found");
			return null;
		}
		
		ArrayList<String> keys = new ArrayList<>(properties.keySet());
		Collections.sort(keys);
		
		int prefixLength = prefix.length();
		int maxLength = 0;
		for(String key : keys) {
			if (key.length() > maxLength) {
				maxLength = key.length();
			}
			
			String value = properties.get(key);
			if (value != null) {
				Matcher matcher = patternDateTime.matcher(value);
				if (matcher.matches()) {
					Instant instant = Instant.parse(value);
					ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
					properties.put(key, dtf.format(zdt));
				}
			}
			
			
			
		}
		
		maxLength = maxLength - prefixLength;
		System.out.println(expand("run." + runName + ".name", prefixLength, maxLength) + " : " + runName);
		for(String key : keys) {
			System.out.println(expand(key, prefixLength, maxLength) + " : " + properties.get(key));
		}

		return null;
	}
	
	public String expand(String string, int prefix, int length) {
		string = string.substring(prefix);
		while(string.length() < length) {
			string += " ";
		}
		return string;
	}
}
