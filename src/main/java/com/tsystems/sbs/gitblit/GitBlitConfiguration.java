package com.tsystems.sbs.gitblit;

import static hudson.model.Items.XSTREAM2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * Represents the global settings of the Gitblit Organization plugin, accessible via Jenkins UI (Manage Jenkins - Configure Systems)
 */
@Extension
public class GitBlitConfiguration extends GlobalConfiguration {
	
	/**
	 * Used for class and package name retrocompatibility.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void addAliases() {
        XSTREAM2.addCompatibilityAlias("com.tsystems.sbs.gitblitbranchsource.GitBlitConfiguration", GitBlitConfiguration.class);
    }

	public static GitBlitConfiguration get() {
		return GlobalConfiguration.all().get(GitBlitConfiguration.class);
	}
	
	private List<Endpoint> endpoints;
	
	public GitBlitConfiguration() {
		load();
	}
	
	@Override
	public boolean configure(StaplerRequest req,JSONObject json) throws FormException {
		req.bindJSON(this,json);
		return true;
	}
	
	public List<Endpoint> getEndpoints() {
		return endpoints == null ? Collections.<Endpoint>emptyList() : Collections.unmodifiableList(endpoints);
	}
	
	public void setEndpoints(List<Endpoint> endpoints){
		endpoints = new ArrayList<Endpoint>(endpoints == null ? Collections.<Endpoint>emptyList() : endpoints);
		//Remove duplicates and empty URLs
		Set<String> gitblitUris = new HashSet<String>();
		for(Iterator<Endpoint> iterator = endpoints.iterator(); iterator.hasNext(); ) {
			Endpoint endpoint = iterator.next();
			if(endpoint.getGitblitUri() == null || gitblitUris.contains(endpoint.getGitblitUri())) {
				iterator.remove();
			}
			gitblitUris.add(endpoint.getGitblitUri());
		}
		this.endpoints = endpoints;
		save();
	}
	
}
