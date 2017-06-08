package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
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
	private final String gitblitUri;
	private final String repository;
	
	private String includes = DescriptorImpl.defaultIncludes;
	private String excludes = DescriptorImpl.defaultExcludes;
	
	@DataBoundConstructor
	public GitBlitSCMSource(String id, String gitblitUri, String repository) {
		super(id);
		this.gitblitUri = gitblitUri;
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
		return repository;
	}
	
	public String getGitblitUri() {
		return gitblitUri;
	}
	
	public String getRepository() {
		return repository;
	}

	@Override
	public String getIncludes() {
		return includes;
	}
	
	@DataBoundSetter
	public void setIncludes(String includes) {
		this.includes = includes;
	}

	@Override
	public String getExcludes() {
		return excludes;
	}
	
	@DataBoundSetter
	public void setExcludes(String excludes) {
		this.excludes = excludes;
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

		public static final String defaultIncludes = "*";
        public static final String defaultExcludes = "";
        
		@Override
		public String getDisplayName() {
			return "GitBlit";
		}
		
		//Jelly (GUI) method
		public ListBoxModel doFillGitblitUriItems() {
			ListBoxModel result = new ListBoxModel();
			for(Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
				result.add(e.getName() == null ? e.getApiUri() : e.getName(), e.getApiUri());
			}
			return result;
		}
		
		//Jelly (GUI) method
		public ListBoxModel doFillRepositoryItems(@QueryParameter String gitblitUri) throws IOException {
			ListBoxModel model = new ListBoxModel();
			
			if (gitblitUri != null && !gitblitUri.isEmpty()) {
				JSONObject response = Connector.listRepositories(gitblitUri);//Maybe we should list the repositories which match the organization pattern
				JSONArray repoURLs = response.names();
				
				
				for(int i=0; i<repoURLs.size(); i++)
					model.add(repoURLs.getString(i));
				
			}
			
			return model;
		}
		
		//Jelly (GUI) method
		public boolean isGitblitUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}
		
	}
	
}
