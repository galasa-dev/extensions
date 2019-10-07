package dev.galasa.core.jenkins.plugin;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class GalasaConfiguration extends GlobalConfiguration {
	
	private String url;
	private String serverCredentials;
	
	public GalasaConfiguration() {
		load();
	}
	
	public static GalasaConfiguration get() {
		return GlobalConfiguration.all().get(GalasaConfiguration.class);
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getServerCredentials() {
		return serverCredentials;
	}
	
	public URL getURL() throws MalformedURLException, AbortException {
		if (url == null) {
			throw new AbortException("The galasa bootstrap URL is missing in global settings");
		}
		
		return new URL(url);
	}
	
	public void setUrl(String url) {
		this.url = url;
		save();
	}
	
	public void setServerCredentials(String serverCredentials) {
		this.serverCredentials = serverCredentials;
		save();
	}
		
	public StandardUsernamePasswordCredentials getCredentials(Run<?, ?> run) throws MalformedURLException, AbortException {
		
		StandardUsernamePasswordCredentials cred = CredentialsProvider.findCredentialById(this.serverCredentials, StandardUsernamePasswordCredentials.class, run);
		return cred;
	}	
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		req.bindJSON(this,json);
		return true;
	}
	
	public FormValidation doCheckUrl(@QueryParameter String value) {
		return FormValidation.ok();
	}
	
	@Override
	public String getDisplayName() {
		return "Galasa Configuration";
	}
	
	
	public ListBoxModel doFillServerCredentialsItems(
	        @AncestorInPath Item item,
	        @QueryParameter String serverCredentials
	        ) {
	  StandardListBoxModel result = new StandardListBoxModel();
	  return result.includeEmptyValue()
	    .includeAs(ACL.SYSTEM, item, StandardUsernamePasswordCredentials.class);
	}
}
