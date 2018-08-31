package com.tsystems.sbs.gitblit;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.plugins.git.extensions.impl.MessageExclusion;
import jenkins.plugins.git.traits.GitSCMExtensionTrait;
import jenkins.plugins.git.traits.GitSCMExtensionTraitDescriptor;

/**
 * Avoids triggering jobs for commits made by the maven release plugin.
 * This avoids repeated consequent builds with changes only in the pom.
 * @author ccapdevi
 *
 */
public class MavenReleaseExclusionTrait extends GitSCMExtensionTrait<MessageExclusion> {
	/**
	 * Stapler constructor.
	 */
	@DataBoundConstructor
	public MavenReleaseExclusionTrait() {
		super(new MessageExclusion("[maven-release-plugin].*"));
	}

	/**
	 * Our {link hudson.model.Descriptor}
	 */
	@Extension
	public static class DescriptorImpl extends GitSCMExtensionTraitDescriptor {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return "Exclude commits with the specified message.";
		}
	}
}
