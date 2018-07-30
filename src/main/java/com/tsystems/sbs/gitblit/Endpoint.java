package com.tsystems.sbs.gitblit;

import static hudson.model.Items.XSTREAM2;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Represents a Gitblit instance to connect to.
 */
public class Endpoint extends AbstractDescribableImpl<Endpoint> {
	private final String name;
	private transient String apiUri;
	private String gitblitUri;
	
	/**
	 * Used for class and package name retrocompatibility.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void addAliases() {
        XSTREAM2.addCompatibilityAlias("com.tsystems.sbs.gitblitbranchsource.Endpoint", Endpoint.class);
    }
	
	/**
	 * Used for field retrocompatibility.
	 * @return this object.
	 */
	private Object readResolve() {
		if (apiUri != null) {
			gitblitUri = apiUri;
		}
		
		return this;
	}
	
	/**
	 * Construct an endpoint with the uri of the Gitblit instance and its alias.
	 * @param gitblitUri The URL of the Gitblit instance.
	 * @param name The alias for the Gitblit instance.
	 */
	@DataBoundConstructor
	public Endpoint(String gitblitUri, String name) {
		this.gitblitUri = Util.fixEmptyAndTrim(gitblitUri);
		this.name = Util.fixEmptyAndTrim(name);
	}
	
	public String getGitblitUri() {
		return gitblitUri;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Endpoint descriptor.
	 */
	@Extension
	public static class DescriptorImpl extends Descriptor<Endpoint> {

		@Override
		public String getDisplayName() {
			return "";
		}
		
		@Restricted(NoExternalUse.class)
		public FormValidation doCheckApiUri(@QueryParameter String gitblitUri) {
			//TODO: form validation (gitblitUri)
			return FormValidation.ok();
		}
		
		@Restricted(NoExternalUse.class)
		public FormValidation doCheckName(@QueryParameter String name){
			//TODO: form validation (name) 
			return FormValidation.ok();
		}
		
	}
	
}
