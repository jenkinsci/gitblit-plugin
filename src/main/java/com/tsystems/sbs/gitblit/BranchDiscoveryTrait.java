package com.tsystems.sbs.gitblit;

import hudson.Extension;
import jenkins.plugins.git.GitSCMSourceContext;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;

/**
 * A {@link Discovery} trait for GitBlit that will discover branches on the repository.
 */
public class BranchDiscoveryTrait extends SCMSourceTrait {
	@Override
	protected void decorateContext(SCMSourceContext<?, ?> context) {
		GitSCMSourceContext<?,?> gitContext = (GitSCMSourceContext<?,?>) context;
		gitContext.wantBranches(true);
	}
	
	/**
     * Our descriptor.
     */
	@Extension
	@Discovery
	public static class DescriptorImpl extends SCMSourceTraitDescriptor {
		@Override
		public String getDisplayName() {
			return "BranchDiscoveryTrait";
		}
	}
}