package com.tsystems.sbs.gitblitbranchsource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTrait;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;

public class GitBlitSCMSource extends GitSCMSource {

	/**
	 * This dictates where the specific URL from which the source will be retrieved.
	 */
	private final String gitblitUri;
	private final String remote;
	
	private List<SCMSourceTrait> traits = new ArrayList<>();
	
	/** Credentials for actual clone; may be SSH private key. */
	private final String checkoutCredentialsId;
	/** Credentials for GitBlit API; currently only supports username/password (personal access token). */
	private final String scanCredentialsId;
	
	@DataBoundConstructor
	public GitBlitSCMSource(String id, String gitblitUri, String checkoutCredentialsId, String scanCredentialsId, String remote, String includes, String excludes) {
		super(remote);
		this.gitblitUri = gitblitUri;
		this.checkoutCredentialsId = checkoutCredentialsId;
		this.scanCredentialsId = scanCredentialsId;
		this.remote = remote;
		
		//SET TRAITS
		//TODO: check which traits could be useful
		List<SCMSourceTrait> traits = new ArrayList<>();
		
		//Branch discovery trait: this allows the navigator to see and process branches
		traits.add(new BranchDiscoveryTrait());
		//Branch filtering trait
		traits.add(new WildcardSCMHeadFilterTrait(includes,excludes));
		setTraits(traits);
	}
	
	@Override
	public String getCredentialsId() {
		if (DescriptorImpl.ANONYMOUS.equals(checkoutCredentialsId)) {
			return null;
		} else if (DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
			return scanCredentialsId;
		} else {
			return checkoutCredentialsId;
		}
	}
	
	public String getScanCredentialsId() {
		return scanCredentialsId;
	}
	
	public String getCheckoutCredentialsId() {
		return checkoutCredentialsId;
	}
	
	/**
	 * This dictates where the specific URL from which the source will be retrieved.
	 */
	@Override
	public String getRemote() {
		return remote;
	}
	
	public String getGitblitUri() {
		return gitblitUri;
	}
	
	@Override
	public List<SCMSourceTrait> getTraits() {
		return traits;
	}
	
	@DataBoundSetter
    public void setTraits(List<SCMSourceTrait> traits) {
        this.traits = SCMTrait.asSetList(traits);
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

        public static final String ANONYMOUS = "ANONYMOUS";
        public static final String SAME = "SAME";
        
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
		
		public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String apiUri) {
            return Connector.listCheckoutCredentials(context, apiUri);
        }

        public ListBoxModel doFillScanCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String apiUri) {
            return Connector.listScanCredentials(context, apiUri);
        }
		
		//Jelly (GUI) method
		public boolean isGitblitUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}
		
	}
	
}
