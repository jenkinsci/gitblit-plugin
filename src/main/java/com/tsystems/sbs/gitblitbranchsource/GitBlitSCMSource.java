package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GitBlitSCMSource extends AbstractGitSCMSource {

	private final String remote;
	
	public GitBlitSCMSource(String id,String remote) {
		super(id);
		this.remote = remote;
	}

	@Override
	public String getCredentialsId() {
		// TODO: Does GitBlit require credentials?
		return null;
	}

	@Override
	public String getRemote() {
		return remote;
	}

	@Override
	public String getIncludes() {
		return ".*";//TODO: make this regex Pattern configurable
	}

	@Override
	public String getExcludes() {
		return "";//TODO: make this regex Pattern configurable
	}

	@Override
	protected List<RefSpec> getRefSpecs() {
		//TODO: don't know how this works. Copy-pasted from GitHubSCMSource
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
		
		public ListBoxModel doFillApiUriItems() {
			ListBoxModel result = new ListBoxModel();
			result.add("GitBlit","");
			for(Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
				result.add(e.getName() == null ? e.getApiUri() : e.getName(), e.getApiUri());
			}
			return result;
		}
		
		public ListBoxModel doFillRepositoryItems(@AncestorInPath Item context, @QueryParameter String apiUri) throws IOException {
			
			JSONObject response = Connector.connect(apiUri);
			JSONArray repoURLs = response.names();
			
			ListBoxModel model = new ListBoxModel();
			
			for(int i=0; i<repoURLs.size(); i++)
				model.add(repoURLs.getString(i));
			
			return model;
		}
		
		public boolean isApiUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}
		
	}
	
}
