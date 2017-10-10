package com.tsystems.sbs.gitblitbranchsource;

import hudson.Extension;
import jenkins.plugins.git.GitSCMSourceContext;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;

public class DiscoverBranchesTrait extends SCMSourceTrait {
	@Override
	protected void decorateContext(SCMSourceContext<?, ?> context) {
		GitSCMSourceContext gitContext = (GitSCMSourceContext) context;
		gitContext.wantBranches(true);
	}
	
	@Extension
	public static class DescriptorImpl extends SCMSourceTraitDescriptor {
		@Override
		public String getDisplayName() {
			return null;
		}
	}
}