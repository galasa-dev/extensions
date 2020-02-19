/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.extensions.jenkins.plugin;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.galasa.framework.api.runs.bind.ScheduleRequest;
import dev.galasa.framework.api.runs.bind.ScheduleStatus;
import hudson.AbortException;
import hudson.model.Run;

public class ApiComms {

    private final PrintStream logger;
    private GalasaConfiguration galasaConfiguration;
    private GalasaContext galasaContext;
    private StandardUsernamePasswordCredentials credentials;
    private Run<?, ?> run;
    private String                              jwt;

    public ApiComms(PrintStream logger, Run<?, ?> run) throws MalformedURLException, AbortException {
        this.logger = logger;
        this.run = run;
        this.galasaConfiguration = GalasaConfiguration.get();
        this.galasaContext = new GalasaContext(galasaConfiguration.getURL(), getCredentials());

    }
    
    public Properties getGalasaProperties() throws IOException, InterruptedException, MissingClass {
        Properties configurationProperties = new Properties();
        
        String host = galasaConfiguration.getUrl();
        if (!host.endsWith("/")) {
            host += "/";
        }

        if (this.jwt == null) {
            logger.println("Authenticating with Galasa auth Service");
            authenticate(new URL(host + "auth"));
        }
        logger.println("Retrieving Galasa Bootstrap Properties");

        HttpGet getRequest = new HttpGet(host + "bootstrap");
        String bootstrapResponse = galasaContext.execute(getRequest, logger);
        configurationProperties.load(new StringReader(bootstrapResponse));
        logger.println("Received Bootsrap properties:");
        configurationProperties.list(logger);

        return configurationProperties;
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

    public String submitTests(ScheduleRequest request, UUID uuid) throws AbortException {
        String scheduleRequestString = null;
        String scheduleResponseString = null;
        try {
            scheduleRequestString = new Gson().toJson(request);

            HttpPost postRequest = new HttpPost(galasaContext.getGalasaURI() + "runs/" + uuid.toString());
            postRequest.addHeader("Accept", "application/json");
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.setEntity(new StringEntity(scheduleRequestString));

            scheduleResponseString = galasaContext.execute(postRequest, logger);
            logger.println("Tests schedule endpoint: " + postRequest.getURI());
            
            return scheduleRequestString;
        } catch (IOException | InterruptedException | MissingClass | URISyntaxException | JsonSyntaxException e) {
            logger.println("Failed to schedule runs '" + e.getMessage());
            logger.println("Schedule request:-");
            logger.println(scheduleRequestString);
            logger.println("Schedule response:-");
            logger.println(scheduleResponseString);
            throw new AbortException("Failed to schedule runs '" + e.getMessage());
        }
    }

    ScheduleStatus getTestStatus(UUID uuid) throws AbortException {
        String scheduleResponseString = null;
        ScheduleStatus scheduleStatus = null;
        try {
            HttpGet getRequest = new HttpGet(this.galasaContext.getGalasaURI() + "runs/" + uuid.toString());
            getRequest.addHeader("Accept", "application/json");

            scheduleResponseString = this.galasaContext.execute(getRequest, logger);
            scheduleStatus = new Gson().fromJson(scheduleResponseString, ScheduleStatus.class);
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
    




}
