/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.extensions.jenkins.plugin;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import dev.galasa.framework.SerializedRun;
import dev.galasa.framework.api.runs.bind.RequestorType;
import dev.galasa.framework.api.runs.bind.ScheduleRequest;
import dev.galasa.framework.api.runs.bind.ScheduleStatus;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class GalasaSharedEnvironmentDiscard extends Builder implements SimpleBuildStep {

    private String                              test;
    private String                              runName;
    private String                              testInstance;
    private String                              stream;
    private String[]                            envProps;
    private Properties                          overrides;
    private int                                 pollTime               = 0;
    private boolean                             trace                  = false;

    private int                                 pollFailures           = 0;
    private final static int                    TOTAL_FAILURES         = 5;

    private String                              obr                    = "";
    private String                              mavenRepository        = "";

    private static final int                    POLL_TIME_DEFAULT      = 30;

    private GalasaConfiguration                 galasaConfiguration;
    private StandardUsernamePasswordCredentials credentials            = null;

    private PrintStream                         logger;

    private Properties                          properties;

    private Run                                 run;
    
    private UUID uuid;

    @DataBoundConstructor
    public GalasaSharedEnvironmentDiscard(String test, String stream, String[] envProps) {
        this.test = test;
        this.stream = stream;
        this.envProps = envProps;
    }

    @DataBoundSetter
    public void setObr(String obr) {
        this.obr = obr;
    }

    @DataBoundSetter
    public void setRunName(String runName) {
        this.runName = runName;
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

        if (this.pollTime == 0) {
            this.pollTime = POLL_TIME_DEFAULT;
        }
        if (this.uuid == null || this.uuid.toString().trim().isEmpty()) {
            this.uuid = UUID.randomUUID();
        }

    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        this.run = run;
        this.logger = listener.getLogger();
        
        setDefaults();

        logger.println("************************************************************");
        logger.println("** Discard Galasa Shared Environment                     ***");
        logger.println("************************************************************");
        
        if (test == null || test.trim().isEmpty()) {
            throw new IOException("Test is missing");
        }
        
        if (runName == null || runName.trim().isEmpty()) {
            throw new IOException("Run name is missing");
        }
        
        ApiComms comms = new ApiComms(this.logger, run);
        
        overrides = new Properties();
        
        //*** Load overrides
        for(String override : this.envProps) {
            override = override.trim();
            if (!override.isEmpty()) {
                int equals = override.indexOf('=');
                if (equals > 0) {
                    this.overrides.put(override.substring(0, equals), override.substring(equals + 1));
                }
            }
        }
        
        if (!overrides.isEmpty()) {
            logger.println("------------------------------------------------");
            logger.println("Properties:-");
            logger.println("------------------------------------------------");
            overrides.list(logger);
        }

        logger.println("Galasa Server Poll time is: " + this.pollTime + " seconds");
        
        submitTestToSchedule(test, runName, this.overrides, comms);
        
        ScheduleStatus lastStatus = null;
        
        boolean testFinished = false;
        while (!testFinished) {
            lastStatus = comms.getTestStatus(uuid);
            if (lastStatus == null) {
                this.pollFailures++;
                if (pollFailures >= TOTAL_FAILURES) {
                    throw new AbortException("Total number of poll attempts exceeds total failures, abandoning run");
                } else
                    continue;
            } else {
            }
            testFinished = lastStatus.getScheduleStatus().isRunComplete();
            if (testFinished)
                break;
            try {
                Thread.sleep(this.pollTime * 1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        
        //*** Check final status of the build
        
        if (lastStatus.getRuns() == null || lastStatus.getRuns().size() != 1) {
            throw new AbortException("Lost build run or there are too many of them for schedule id " + this.uuid.toString());
        }
        
        SerializedRun runResult = lastStatus.getRuns().get(0);
        
        if (!"finished".equalsIgnoreCase(runResult.getStatus())) {
            logger.println("Shared Environment Discard failed - " + runResult.getName());
            run.setResult(Result.FAILURE);
        }
        
        logger.println("Shared Environment discard suceeded - " + runResult.getName());
        run.setResult(Result.SUCCESS);
    }
    
    @Symbol("galasasb")
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
            return "Galasa - Discard Shared Environment";
        }
    }


    private String submitTestToSchedule(String test, 
            String runName,
            Properties overrides,
            ApiComms comms) throws AbortException {
        
        ScheduleRequest request = new ScheduleRequest();
        request.setRequestorType(RequestorType.JENKINS);
        request.setTestStream(this.stream);
        request.setClassNames(new ArrayList<String>());
        request.setTrace(this.trace);
        request.setSharedEnvironmentPhase("DISCARD");
        request.setSharedEnvironmentRunName(runName);

        if (!this.mavenRepository.equals("")) {
            request.setMavenRepository(this.mavenRepository);
        }

        if (!this.obr.equals("")) {
            request.setObr(this.obr);
        }
        
        request.getClassNames().add(test);
        
        request.setRunProperties(overrides);
        
        return comms.submitTests(request, uuid);
    }



}
