package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GitBlitSCMSource extends AbstractGitSCMSource {

	/**
	 * This dictates where the specific URL from which the source will be retrieved.
	 */
	private final String apiUri;
	private final String repository;
	
	@DataBoundConstructor
	public GitBlitSCMSource(String id, String apiUri, String repository) {
		super(id);
		this.apiUri = apiUri;
		this.repository = repository;
	}

	@Override
	public String getCredentialsId() {
		return null;
	}

	/**
	 * This dictates where the specific URL from which the source will be retrieved.
	 */
	@Override
	public String getRemote() {
		return apiUri;
	}
	
	public String getApiUri() {
		return apiUri;
	}
	
	public String getRepository() {
		return repository;
	}

	@Override
	public String getIncludes() {
		return "*";//TODO: make this regex Pattern configurable
	}

	@Override
	public String getExcludes() {
		return "";//TODO: make this regex Pattern configurable
	}
	
	@Override
	protected List<RefSpec> getRefSpecs() {
		/*
		 * The format of the refspec is an optional +, followed by <src>:<dst>, where <src> is the pattern for references 
		 * on the remote side and <dst> is where those references will be written locally. The + tells Git to update 
		 * the reference even if it isn’t a fast-forward.
		 * 
		 * (taken from Git docs: https://git-scm.com/book/en/v2/Git-Internals-The-Refspec)
		 */
		return new ArrayList<>(Arrays.asList(new RefSpec("+refs/heads/*:refs/remotes/origin/*"),
	            // For PRs we check out the head, then perhaps merge with the base branch.
	            new RefSpec("+refs/pull/*/head:refs/remotes/origin/pr/*")));
	}
	
	@Symbol("gitblit")
	@Extension
	public static class DescriptorImpl extends SCMSourceDescriptor {

		@Override
		public String getDisplayName() {
			return "GitBlit";
		}
		
		//Jelly (GUI) method
		public ListBoxModel doFillApiUriItems() {
			ListBoxModel result = new ListBoxModel();
			for(Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
				result.add(e.getName() == null ? e.getApiUri() : e.getName(), e.getApiUri());
			}
			return result;
		}
		
		//Jelly (GUI) method
		public ListBoxModel doFillRepositoryItems(@QueryParameter String apiUri) throws IOException {
			
			JSONObject response = Connector.connect(apiUri);
			JSONArray repoURLs = response.names();
			
			ListBoxModel model = new ListBoxModel();
			
			for(int i=0; i<repoURLs.size(); i++)
				model.add(repoURLs.getString(i));
			
			return model;
		}
		
		//Jelly (GUI) method
		public boolean isApiUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}
		
	}
	
}
