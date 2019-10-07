package dev.galasa.core.jenkins.plugin;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.galasa.framework.SerializedRun;
import dev.galasa.framework.api.runs.bind.RequestorType;
import dev.galasa.framework.api.runs.bind.RunStatus;
import dev.galasa.framework.api.runs.bind.ScheduleRequest;
import dev.galasa.framework.api.runs.bind.ScheduleStatus;
import dev.galasa.framework.api.runs.bind.Status;
import dev.galasa.framework.api.runs.bind.TestCase;
import dev.galasa.framework.api.runs.bind.TestCaseResult;
import dev.galasa.framework.api.runs.bind.TestRun;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;


public class GalasaTestExecution extends Builder implements SimpleBuildStep{

	private String[] tests;
	private String testInstance;
	private String stream;
	private String envProps;
	private Properties overrides;
	private int numberOfRuns = 0;
	private int pollTime = 0;
	
	private int pollFailures = 0;
	private final static int TOTAL_FAILURES = 5;
	
	private String obr = "";
	private String mavenRepository = "";
	
	private static final int NUMBER_OF_RUNS_DEFAULT = 1;
	private static final int POLL_TIME_DEFAULT = 120;
	
	private GalasaConfiguration galasaConfiguration;
	
	private PrintStream logger;
	
	private HashMap<String, TestCase> currentTests = new HashMap<>();
	
	private LinkedList<TestCase> testToRun = new LinkedList<TestCase>();
	private LinkedList<TestCase> runningTests = new LinkedList<TestCase>();
	private ArrayList<TestCase> finishedTests = new ArrayList<TestCase>();
	private ArrayList<TestCase> failedTests = new ArrayList<TestCase>();
	
	private Properties properties;
	
	private Run run;
	private String jwt;
	
	private UUID uuid;

	@DataBoundConstructor
	public GalasaTestExecution(String[] tests, String stream, String envProps) {
		this.tests = tests.clone();
		this.stream = stream;
		this.envProps = envProps;
		
		setDefaults();
	}
	
	@DataBoundSetter
	public void setObr(String obr) {
		this.obr = obr;
	}
	
	@DataBoundSetter
	public void setMavenRepository(String mavenRepository) {
		this.mavenRepository = mavenRepository;
	}
	
	@DataBoundSetter
	public void setTestInstance(String testInstance) {
		this.testInstance = testInstance;
	}
	
	@DataBoundSetter
	public void setNumberOfRuns(String numberOfRuns) {
		try {
			this.numberOfRuns = Integer.parseInt(numberOfRuns);
		}catch(NumberFormatException nfe) {
			this.numberOfRuns = NUMBER_OF_RUNS_DEFAULT;
		}
		
	}
	
	@DataBoundSetter
	public void setPollTime(String pollTime) {
		try {
			this.pollTime = Integer.parseInt(pollTime);
		}catch(NumberFormatException nfe) {
			this.pollTime = POLL_TIME_DEFAULT;
		}
		
	}

	private void setDefaults() {
		if (this.stream == null || this.stream.trim().isEmpty()) {
			this.stream = "prod";
		}

		if (this.testInstance == null || this.testInstance.trim().isEmpty()) {
			this.testInstance = "default";
		}

		if (this.envProps == null || envProps.trim().isEmpty()) {
			this.envProps = "";
		}
		
		if(this.numberOfRuns == 0) {
			this.numberOfRuns = NUMBER_OF_RUNS_DEFAULT;
		}
		
		if(this.pollTime == 0) {
			this.pollTime = POLL_TIME_DEFAULT;
		}
		if(this.uuid == null || this.uuid.toString().trim().isEmpty()) {
			this.uuid = UUID.randomUUID();
		}
		
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		this.run = run;
		this.logger = listener.getLogger();

		logger.println("************************************************************");
		logger.println("** Galasa Test Selection starting                        ***");
		logger.println("************************************************************");
		galasaConfiguration = GalasaConfiguration.get();

		TestRun testRun = TestRun.getTestRun("jenkins");

		properties = new Properties();
		overrides = new Properties();
		
		// *** Retrieve the JAT properties
		properties.putAll(getGalasaProperties(galasaConfiguration.getUrl(),getCredentials()));

		// add envProps to overrideString if specified
		if (this.envProps != null && !this.envProps.trim().isEmpty()) {
			properties.load(new StringReader(envProps));
		}

		// *** Select tests
		if (tests != null) {
			for (String tc : tests) {
				String className = tc.trim();
				if (!className.isEmpty() && !className.startsWith("!")) {
					addTest(currentTests, testRun, stream, className, properties);
				}
			}
		}

		if (currentTests.isEmpty()) {
			logger.println("No tests have been selected for running");
			return;
		}

		logger.println("The following " + currentTests.size() + " Voras test classes have been selected for running");
		for (TestCase tc : currentTests.values()) {
			logger.println("    " + getTestNamePart(tc.className));
		}

		logger.println("Test Run Instance = " + testRun.instance);

		if (!overrides.isEmpty()) {
			logger.println("------------------------------------------------");
			logger.println("Properties:-");
			logger.println("------------------------------------------------");
			overrides.list(logger);
		}
		
		logger.println("Maximum number of concurrent runs is: " + this.numberOfRuns);
		logger.println("Voras Server Poll time is: " + this.pollTime + " seconds");
		
		for(TestCase tc : testRun.getTestCases()) {
			testToRun.add(tc);
		}
		
		
		VorasContext vorasContext = new VorasContext(galasaConfiguration.getURL(), getCredentials());
		

		submitAllTestsToSchedule(testToRun, vorasContext);
		
		if(!waitForTestsToRun(vorasContext)) {
			testRun.status = Status.BYPASSED;
			return;
		}
		
		if (!finishedTests.isEmpty()) {
			logger.println("The following test PASSED:-");
			for(TestCase test : finishedTests) {
				logger.println("    " + test.getBundleName() + "/" + test.getClassName());
			}
		}
		
		if (!failedTests.isEmpty()) {
			logger.println("The following test FAILED:-");
			for(TestCase test : failedTests) {
				StringBuilder sb = new StringBuilder();
				for(TestCaseResult tcr : test.getResults()) {
					if (tcr.runIdFriendly != null && !tcr.runIdFriendly.isEmpty()) {
						if (sb.length() > 0) {
							sb.append(",");
						}
						sb.append(tcr.runIdFriendly);
					}
				}
				
				String runs = "";
				if (sb.length() > 0) {
					runs = " (" + sb.toString() + ")";
				}
				
				logger.println("    " + test.getBundleName() + "/" + test.getClassName() + runs);
			}
		}


		if (!failedTests.isEmpty()) {
			testRun.status = Status.FAILED;
		} else {
			testRun.status = Status.SUCCESS;
		}
	}
	
	private void submitAllTestsToSchedule(LinkedList<TestCase> testsToSubmit, VorasContext context) throws AbortException {
		ScheduleRequest request = new ScheduleRequest();
		request.requestorType = RequestorType.JENKINS;
		request.testStream = this.stream;
		request.classNames = new ArrayList<String>();
		
		if(!this.mavenRepository.equals("")) {
			request.mavenRepository = this.mavenRepository;
			request.obr = this.obr;
		}

		for(TestCase tc : testsToSubmit) {
			request.classNames.add(tc.getFullName());
		}

		String scheduleRequestString = null;
		String scheduleResponseString = null;
		try {
			scheduleRequestString = new Gson().toJson(request);

			HttpPost postRequest = new HttpPost(context.getVorasURI()+"schedule/"+this.uuid.toString());
			postRequest.addHeader("Accept", "application/json");
			postRequest.addHeader("Content-Type", "application/json");
			postRequest.setEntity(new StringEntity(scheduleRequestString));

			scheduleResponseString = context.execute(postRequest, logger);
			logger.println("Tests scheduled: " + scheduleResponseString);

		} catch(IOException|InterruptedException|MissingClass|URISyntaxException|JsonSyntaxException e) {
			logger.println("Failed to schedule runs '" + e.getMessage());
			logger.println("Schedule request:-");
			logger.println(scheduleRequestString);
			logger.println("Schedule response:-");
			logger.println(scheduleResponseString);
			throw new AbortException("Failed to schedule runs '" + e.getMessage());
		}
	}



	private boolean waitForTestsToRun(VorasContext context) throws AbortException {
		boolean testFinished = false;
		while(!testFinished) {
			ScheduleStatus status = getTestStatus(context);
			if(status == null) {
				this.pollFailures++;
				if(pollFailures >= this.TOTAL_FAILURES) {
					logger.println("Total number of poll attempts exceeds total failures, abandoning run");
					return false;
				}
				else
					continue;
			}
			testFinished = status.scheduleStatus.isRunComplete();
			if(testFinished)
				return true;
			try {
				Thread.sleep(this.pollTime * 1000);
			}catch(InterruptedException e) {
				Thread.interrupted();
			}
		}
		return false;
	}
	
	private ScheduleStatus getTestStatus(VorasContext context) throws AbortException {
		String          scheduleResponseString = null;
		ScheduleStatus  scheduleStatus = null;
		try {
			HttpGet getRequest = new HttpGet(context.getVorasURI()+"schedule/" + this.uuid.toString());
			getRequest.addHeader("Accept", "application/json");

			scheduleResponseString = context.execute(getRequest, logger);
			scheduleStatus = new Gson().fromJson(scheduleResponseString, ScheduleStatus.class);
			updateTestStatus(scheduleStatus, logger, runningTests, finishedTests, failedTests);
			return scheduleStatus;
		} catch(AbortException e) {
			throw e;
		} catch(IOException e) {
			logger.println("Failed to inquire schedule '" + e.getMessage() + "'");
			logger.println("Schedule response:-");
			logger.println(scheduleResponseString);
			logger.println("Stacktrace ");
			e.printStackTrace(logger);
			return null;
		} catch(Exception e) {
			logger.println("Failed to inquire schedule '" + e.getMessage() + "'");
			logger.println("Schedule response:-");
			logger.println(scheduleResponseString);
			logger.println("Stacktrace ");
			e.printStackTrace(logger);
			return null;
		}
	}
	
	private StandardUsernamePasswordCredentials getCredentials() throws MalformedURLException, AbortException {
		// *** Find the username and password to use for the Galasa Bootsrap
		StandardUsernamePasswordCredentials credentials = galasaConfiguration.getCredentials(run);
		if (credentials != null) {
			logger.println("Using username '" + credentials.getUsername() + "' for Voras bootstrap");
		} else {
			logger.println("No credentials provided for Voras bootsrap");
		}
		return credentials;
	}
	
	private void authenticate(URL endpoint) {
		try {
			StandardUsernamePasswordCredentials credentials = galasaConfiguration.getCredentials(run);
			Executor executor = Executor.newInstance().auth(credentials.getUsername(), credentials.getPassword().getPlainText());
			this.jwt = executor.execute(Request.Get(endpoint.toURI())).returnContent().asString();
		} catch (ClientProtocolException e) {
			if (e.getMessage().contains("Unauthorized")) {
				logger.println("Unauthorised to access the Galasa bootstrap");
				return;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Properties getGalasaProperties(String host,StandardUsernamePasswordCredentials credentials) throws MalformedURLException {
		Properties configurationProperties = new Properties();
		
		URL authURL = new URL(host + "auth");
		
		if(this.jwt == null)
			authenticate(authURL);
			logger.println("Retrieving Galasa Configuration Properties");
		return configurationProperties;
	}

	private void addTest(HashMap<String, TestCase> currentTests, TestRun testRun,
			String stream, String testFullName, Properties envOverrides) throws IOException {

		if (currentTests.containsKey(testFullName)) {
			return;
		}

		TestCase newTest = new TestCase();
		newTest.bundleName = testFullName.split("/")[0];
		newTest.className = testFullName.split("/")[1];
		newTest.stream = stream;
		newTest.type = "Voras";

		Properties newTestEnvProperties = new Properties();
		newTestEnvProperties.putAll(envOverrides);

		// *** Store properties in String
		StringWriter envPropertiesSw = new StringWriter();
		newTestEnvProperties.store(envPropertiesSw, null);

		currentTests.put(testFullName, newTest);
		testRun.getTestCases().add(newTest);
	}
	
	private boolean updateTestStatus(ScheduleStatus scheduleStatus, PrintStream logger, LinkedList<TestCase> runningTests, ArrayList<TestCase> finishedTests, ArrayList<TestCase> failedTests) throws IOException {
		if (scheduleStatus == null) {
			return false;
		}

		boolean updated = false;

		for (SerializedRun run : scheduleStatus.runs) {
			switch (run.getStatus()) {
			case "ABORTED":
			case "FAILED_RUN":
			case "FAILED_DEFECTS_RUN":
			case "FINISHED_RUN":
			case "FINISHED_DEFECTS_RUN":
			case "IGNORED_RUN":
			case "UNKNOWN":
				if (markTestFinished(run, runningTests,finishedTests, failedTests, logger)) {
					updated = true;
				}
				break;
			case "ALLOCATED":
			case "BUILDING_ENVIRONMENT":
			case "DISCARDING_ENVIRONMENT":
			case "STARTED_RUN":
			case "STARTING_ENVIRONMENT":
			case "STOPPING_ENVIRONMENT":
			case "SUBMITTED":
			case "TESTING":
			default:
				if (reportTestProgress(run, runningTests, logger)) {
					updated = true;
				}
				break;
			}

		}

		return updated;
	}
	
	private boolean markTestFinished(SerializedRun run, LinkedList<TestCase> runningTests, ArrayList<TestCase> finishedTests, ArrayList<TestCase> failedTests, PrintStream logger) {
		Iterator<TestCase> runningTestsi = runningTests.iterator();
		while(runningTestsi.hasNext()) {
			
			TestCase runningTest = runningTestsi.next();
			TestCaseResult tcr = runningTest.getResults().get(0);
			UUID runUUID = UUID.fromString(tcr.runId);
			if (runUUID.toString().equals(run.getGroup())) {
				runningTestsi.remove();
				if (run.getStatus().equals(RunStatus.FINISHED_RUN.toString())  || run.getStatus().equals(RunStatus.FINISHED_DEFECTS_RUN.toString())  || run.getStatus().equals(RunStatus.IGNORED_RUN.toString())) {
					finishedTests.add(runningTest);
				} else {
					failedTests.add(runningTest);
				}
				tcr.status = run.getStatus();
				logger.println("Test " + getTestNamePart(runningTest.className) + " run(" + run.getName() + ")  has finished with " + run.getStatus());
				return true;
			}
		}

		return false;
	}
	
	private boolean reportTestProgress(SerializedRun run, LinkedList<TestCase> runningTests, PrintStream logger) {

		Iterator<TestCase> runningTestsi = runningTests.iterator();
		while(runningTestsi.hasNext()) {
			TestCase runningTest = runningTestsi.next();
			TestCaseResult tcr = runningTest.getResults().get(0);
			UUID runUUID = UUID.fromString(tcr.runId);
			if (runUUID.toString().equals(run.getGroup().toString())) {
				logger.println("Test " + getTestNamePart(runningTest.className) + " run(" + run.getName() + ") is currently " + run.getStatus());
				if (tcr.status.equals(run.getStatus())) {
					return false;
				} else {
					tcr.status = run.getStatus();
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getTestInstance() {
		return testInstance;
	}

	public String getStream() {
		return stream;
	}

	public String getEnvProps() {
		return envProps;
	}

	public static String getTestNamePart(String testName) {
		String[] parts = testName.split("/");
		if (parts.length == 1) {
			return testName;
		}
		return parts[1];
	}

	@Symbol("galasa")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Galasa - Test Class Selection";
		}
	}
	
	private static final class VorasContext {
		private final URL vorasURL;
		private final HttpHost target;
		private final CloseableHttpClient client;
		private final HttpClientContext context;


		private VorasContext(URL url, StandardUsernamePasswordCredentials credentials) {
			this.vorasURL = url;

			this.target = new HttpHost(vorasURL.getHost(), vorasURL.getPort(), vorasURL.getProtocol());

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(this.target.getHostName(), this.target.getPort()),
					new UsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword().getPlainText()));

			this.client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(this.target, basicAuth);

			// Add AuthCache to the execution context
			this.context = HttpClientContext.create();
			this.context.setAuthCache(authCache);

			return;
		}
		
		public String execute(HttpRequest request, PrintStream logger) throws IOException, InterruptedException, MissingClass {
			CloseableHttpResponse response = null;
			String responseString;
			while(true) {
				try {
					response = this.client.execute(this.target, request, this.context);
					responseString = EntityUtils.toString(response.getEntity());

					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						logger.println("Error with call to Galasa WAS " + response.getStatusLine());
					} else {
						break;
					}
				} catch(SocketException e) {
					logger.println("Galasa Server is not responding");
				}

				Thread.sleep(30000);
			}

			return responseString;
		}


		public URI getVorasURI() throws URISyntaxException {
			return vorasURL.toURI();
		}
	}
}
