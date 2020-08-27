/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.extensions.jenkins.plugin;

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
import java.util.Collection;
import java.util.HashMap;
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

import dev.galasa.api.run.Run;
import dev.galasa.api.runs.ScheduleRequest;
import dev.galasa.api.runs.ScheduleStatus;
import dev.galasa.extensions.jenkins.plugin.bind.Status;
import dev.galasa.extensions.jenkins.plugin.bind.TestCase;
import dev.galasa.extensions.jenkins.plugin.bind.TestRun;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class GalasaTestExecution extends Builder implements SimpleBuildStep {

    private String[]                            tests;
    private String                              testInstance;
    private String                              stream;
    private String[]                            envProps;
    private Properties                          overrides;
    private int                                 numberOfRuns           = 0;
    private int                                 pollTime               = 0;
    private boolean                             trace                  = false;

    private int                                 pollFailures           = 0;
    private final static int                    TOTAL_FAILURES         = 5;

    private String                              obr                    = "";
    private String                              mavenRepository        = "";

    private static final int                    NUMBER_OF_RUNS_DEFAULT = 1;
    private static final int                    POLL_TIME_DEFAULT      = 30;

    private GalasaConfiguration                 galasaConfiguration;
    private StandardUsernamePasswordCredentials credentials            = null;

    private PrintStream                         logger;

    private HashMap<String, TestCase>           currentTests           = new HashMap<>();
    private int                                 totalTests             = 0;
    private int                                 failedTests            = 0;

    private Properties                          properties;

    private hudson.model.Run<?,?>               run;
    private String                              jwt;

    private UUID                                uuid;
    
    private final Gson        gson = GalasaGsonBuilder.build();

    @DataBoundConstructor
    public GalasaTestExecution(String[] tests, String stream, String[] envProps) {
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
        } catch (NumberFormatException nfe) {
            this.numberOfRuns = NUMBER_OF_RUNS_DEFAULT;
        }

    }

    @DataBoundSetter
    public void setPollTime(String pollTime) {
        try {
            this.pollTime = Integer.parseInt(pollTime);
        } catch (NumberFormatException nfe) {
            this.pollTime = POLL_TIME_DEFAULT;
        }

    }

    @DataBoundSetter
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    private void setDefaults() {
        if (this.stream == null || this.stream.trim().isEmpty()) {
            this.stream = "prod";
        }

        if (this.testInstance == null || this.testInstance.trim().isEmpty()) {
            this.testInstance = "default";
        }

        if (this.envProps == null) {
            this.envProps = new String[0];
        }

        if (this.numberOfRuns == 0) {
            this.numberOfRuns = NUMBER_OF_RUNS_DEFAULT;
        }

        if (this.pollTime == 0) {
            this.pollTime = POLL_TIME_DEFAULT;
        }
        if (this.uuid == null || this.uuid.toString().trim().isEmpty()) {
            this.uuid = UUID.randomUUID();
        }

    }

    @Override
    public void perform(hudson.model.Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        this.run = run;

        logger.println("************************************************************");
        logger.println("** Galasa Test Selection starting                        ***");
        logger.println("************************************************************");
        galasaConfiguration = GalasaConfiguration.get();
        GalasaContext galasaContext = new GalasaContext(galasaConfiguration.getURL(), getCredentials());

        TestRun testRun = TestRun.getTestRun("jenkins");

        properties = new Properties();
        overrides = new Properties();

        // *** Retrieve the Galasa bootstrap properties
        try {
            properties.putAll(getGalasaProperties(galasaConfiguration.getUrl(), getCredentials(), galasaContext));
        } catch (MissingClass e) {
            logger.println("Unable to access Bootstrap properties");
            return;
        }

        // add envProps to overrideString if specified
        for(String override : this.envProps) {
            override = override.trim();
            if (!override.isEmpty()) {
                int equals = override.indexOf('=');
                if (equals > 0) {
                    this.overrides.put(override.substring(0, equals), override.substring(equals + 1));
                }
            }
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

        this.totalTests = currentTests.size();
        logger.println("The following " + this.totalTests + " Galasa test classes have been selected for running");
        for (TestCase tc : currentTests.values()) {
            logger.println("    " + getTestNamePart(tc.getClassName()));
        }

        logger.println("Test Run Instance = " + testRun.getInstance());

        if (!overrides.isEmpty()) {
            logger.println("------------------------------------------------");
            logger.println("Properties:-");
            logger.println("------------------------------------------------");
            overrides.list(logger);
        }

        logger.println("Maximum number of concurrent runs is: " + this.numberOfRuns);
        logger.println("Galasa Server Poll time is: " + this.pollTime + " seconds");

        submitAllTestsToSchedule(currentTests.values(), galasaContext);

        if (!waitForTestsToRun(galasaContext)) {
            testRun.setStatus(Status.BYPASSED);
            return;
        }

        updateConsole();
        if (this.failedTests > 0) {
            testRun.setStatus(Status.FAILED);
            run.setResult(Result.FAILURE);
        } else {
            testRun.setStatus(Status.SUCCESS);
            run.setResult(Result.SUCCESS);
        }
    }

    private void updateConsole() {
        int completedTests = 0;
        this.failedTests = 0;

        for (TestCase test : currentTests.values()) {
            if (test.getRunDetails().getStatus().equals("finished")) {
                completedTests++;
            }
        }
        logger.println("Galasa has completed " + completedTests + " out of " + totalTests + " tests");

        logger.println("Tests passed:");
        for (TestCase test : currentTests.values()) {
            if ("Passed".equals(test.getRunDetails().getResult())) {
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
        }

        logger.println("Tests failed:");
        for (TestCase test : currentTests.values()) {
            if ("Failed".equals(test.getRunDetails().getResult())) {
                this.failedTests++;
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
            if ("EnvFail".equals(test.getRunDetails().getResult())) {
                this.failedTests++;
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
        }

        logger.println("Tests ignored:");
        for (TestCase test : currentTests.values()) {
            if ("Ignored".equals(test.getRunDetails().getResult())) {
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
        }
        logger.println("Tests lost:");
        for (TestCase test : currentTests.values()) {
            if ("UNKNOWN".equals(test.getRunDetails().getResult())) {
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
            if (test.getRunDetails().getResult() == null) {
                this.failedTests++;
                logger.println(test.getFullName() + " - RunID(" + test.getRunDetails().getName() + ")");
            }
        }
    }

    private void submitAllTestsToSchedule(Collection<TestCase> collection, GalasaContext context)
            throws AbortException {
        ScheduleRequest request = new ScheduleRequest();
        request.setRequestorType("JENKINS");
        request.setTestStream(this.stream);
        request.setClassNames(new ArrayList<String>());
        request.setTrace(this.trace);
        request.setOverrides(this.overrides);

        if (!this.mavenRepository.equals("")) {
            request.setMavenRepository(this.mavenRepository);
        }

        if (!this.obr.equals("")) {
            request.setObr(this.obr);
        }

        for (TestCase tc : collection) {
            request.getClassNames().add(tc.getFullName());
        }

        String scheduleRequestString = null;
        String scheduleResponseString = null;
        try {
            scheduleRequestString = gson.toJson(request);

            HttpPost postRequest = new HttpPost(context.getGalasaURI() + "runs/" + this.uuid.toString());
            postRequest.addHeader("Accept", "application/json");
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.setEntity(new StringEntity(scheduleRequestString));

            scheduleResponseString = context.execute(postRequest, logger);
            logger.println("Tests schedule endpoint: " + postRequest.getURI());

        } catch (IOException | InterruptedException | MissingClass | URISyntaxException | JsonSyntaxException e) {
            logger.println("Failed to schedule runs '" + e.getMessage());
            logger.println("Schedule request:-");
            logger.println(scheduleRequestString);
            logger.println("Schedule response:-");
            logger.println(scheduleResponseString);
            throw new AbortException("Failed to schedule runs '" + e.getMessage());
        }
    }

    private boolean waitForTestsToRun(GalasaContext context) throws AbortException {
        boolean testFinished = false;
        while (!testFinished) {
            ScheduleStatus status = getTestStatus(context);
            if (status == null) {
                this.pollFailures++;
                if (pollFailures >= TOTAL_FAILURES) {
                    logger.println("Total number of poll attempts exceeds total failures, abandoning run");
                    return false;
                } else
                    continue;
            }
            if (status.isComplete())
                return true;
            try {
                Thread.sleep(this.pollTime * 1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        return false;
    }

    private ScheduleStatus getTestStatus(GalasaContext context) throws AbortException {
        String scheduleResponseString = null;
        ScheduleStatus scheduleStatus = null;
        try {
            HttpGet getRequest = new HttpGet(context.getGalasaURI() + "runs/" + this.uuid.toString());
            getRequest.addHeader("Accept", "application/json");

            scheduleResponseString = context.execute(getRequest, logger);
            scheduleStatus = gson.fromJson(scheduleResponseString, ScheduleStatus.class);
            updateTestStatus(scheduleStatus, logger);
            return scheduleStatus;
        } catch (AbortException e) {
            throw e;
        } catch (IOException e) {
            logger.println("Failed to inquire schedule '" + e.getMessage() + "'");
            logger.println("Schedule response:-");
            logger.println(scheduleResponseString);
            logger.println("Stacktrace ");
            e.printStackTrace(logger);
            return null;
        } catch (Exception e) {
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
        if (this.credentials != null)
            return this.credentials;
        this.credentials = galasaConfiguration.getCredentials(run);
        if (credentials != null) {
            logger.println("Using username '" + credentials.getUsername() + "' for Galasa bootstrap");
        } else {
            logger.println("No credentials provided for Galasa bootsrap");
        }
        return credentials;
    }

    private void authenticate(URL endpoint) {
        try {
            StandardUsernamePasswordCredentials credentials = getCredentials();
            Executor executor = Executor.newInstance().auth(credentials.getUsername(),
                    credentials.getPassword().getPlainText());
            this.jwt = executor.execute(Request.Get(endpoint.toURI())).returnContent().asString();
        } catch (ClientProtocolException e) {
            if (e.getMessage().contains("Unauthorized")) {
                logger.println("Unauthorised to access Galasa");
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

    private Properties getGalasaProperties(String host, StandardUsernamePasswordCredentials credentials,
            GalasaContext context) throws IOException, InterruptedException, MissingClass {
        Properties configurationProperties = new Properties();

        if (this.jwt == null) {
            logger.println("Authenticating with Galasa auth Service");
            authenticate(new URL(host + "auth"));
        }
        logger.println("Retrieving Galasa Bootstrap Properties");

        HttpGet getRequest = new HttpGet(host + "bootstrap");
        String bootstrapResponse = context.execute(getRequest, logger);
        configurationProperties.load(new StringReader(bootstrapResponse));
        logger.println("Received Bootsrap properties:");
        configurationProperties.list(logger);

        return configurationProperties;
    }

    private void addTest(HashMap<String, TestCase> currentTests, TestRun testRun, String stream, String testFullName,
            Properties envOverrides) throws IOException {

        if (currentTests.containsKey(testFullName)) {
            return;
        }

        TestCase newTest = new TestCase();
        newTest.setBundleName(testFullName.split("/")[0]);
        newTest.setClassName(testFullName.split("/")[1]);
        newTest.setStream(stream);
        newTest.setType("Galasa");

        Properties newTestEnvProperties = new Properties();
        newTestEnvProperties.putAll(envOverrides);

        // *** Store properties in String
        StringWriter envPropertiesSw = new StringWriter();
        newTestEnvProperties.store(envPropertiesSw, null);

        currentTests.put(testFullName, newTest);
    }

    private boolean updateTestStatus(ScheduleStatus scheduleStatus, PrintStream logger) throws IOException {
        if (scheduleStatus == null) {
            return false;
        }

        boolean updated = false;

        for (Run run : scheduleStatus.getRuns()) {
            TestCase test = currentTests.get(run.getTest());
            Run oldRunDetails = test.getRunDetails();
            if (oldRunDetails != null) {
                if (!oldRunDetails.getStatus().equals(run.getStatus())) {
                    updated = true;
                }
            }
            test.setRunDetails(run);
        }
        return updated;
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

    public String[] getEnvProps() {
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

    private static final class GalasaContext {
        private final URL                 galasaURL;
        private final HttpHost            target;
        private final CloseableHttpClient client;
        private final HttpClientContext   context;

        private GalasaContext(URL url, StandardUsernamePasswordCredentials credentials) {
            this.galasaURL = url;

            this.target = new HttpHost(galasaURL.getHost(), galasaURL.getPort(), galasaURL.getProtocol());

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(this.target.getHostName(), this.target.getPort()),
                    new UsernamePasswordCredentials(credentials.getUsername(),
                            credentials.getPassword().getPlainText()));

            this.client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(this.target, basicAuth);

            // Add AuthCache to the execution context
            this.context = HttpClientContext.create();
            this.context.setAuthCache(authCache);

            return;
        }

        public String execute(HttpRequest request, PrintStream logger)
                throws IOException, InterruptedException, MissingClass {
            CloseableHttpResponse response = null;
            String responseString;
            while (true) {
                try {
                    response = this.client.execute(this.target, request, this.context);
                    responseString = EntityUtils.toString(response.getEntity());

                    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        logger.println("Error with call to Galasa WAS " + response.getStatusLine());
                    } else {
                        break;
                    }
                } catch (SocketException e) {
                    logger.println("Galasa Server is not responding");
                }

                Thread.sleep(30000);
            }

            return responseString;
        }

        public URI getGalasaURI() throws URISyntaxException {
            return galasaURL.toURI();
        }
    }
}
