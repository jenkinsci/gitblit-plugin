package com.tsystems.sbs.gitblit;

import static hudson.model.Items.XSTREAM2;

import java.net.Proxy;
import java.util.List;

import javax.annotation.CheckForNull;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * Connection utilities.
 */
public class Connector {
	
	/**
	 * Used for class and package name retrocompatibility.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void addAliases() {
        XSTREAM2.addCompatibilityAlias("com.tsystems.sbs.gitblitbranchsource.Connector", Connector.class);
    }
	
	private Connector() {
		throw new IllegalAccessError("Utility class");
	}
	
	static List<DomainRequirement> gitblitDomainRequirements(String gitblitUri) {
		return URIRequirementBuilder.fromUri(gitblitUri).build();
	}
   
   /**
    * Resolves the specified scan credentials in the specified context for use against the specified API endpoint.
    *
    * @param context           the context.
    * @param gitblitUri            the API endpoint.
    * @param credentialsId the credentials to resolve.
    * @return the {@link StandardCredentials} or {@code null}
    */
   @CheckForNull
   public static StandardCredentials lookupCredentials(@CheckForNull Item context,
                                                           @CheckForNull String gitblitUri,
                                                           @CheckForNull String credentialsId) {
       if (Util.fixEmpty(credentialsId) == null) {
           return null;
       } else {
           return CredentialsMatchers.firstOrNull(
               CredentialsProvider.lookupCredentials(
                   StandardUsernameCredentials.class,
                   context,
                   context instanceof Queue.Task
                           ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                           : ACL.SYSTEM,
                   gitblitDomainRequirements(gitblitUri)
               ),
               CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId), gitblitCredentialsMatcher())
           );
       }
   }
   
   private static CredentialsMatcher gitblitCredentialsMatcher() {
       // TODO OAuth credentials
	   return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
   }
   
   /**
    * Populates a {@link ListBoxModel} with the checkout credentials appropriate for the supplied context against the
    * supplied API endpoint.
    *
    * @param context the context.
    * @param gitblitUri  the gitblit server.
    * @return a {@link ListBoxModel}.
    */
   public static ListBoxModel listCredentials( Item context, String gitblitUri) {
       return new StandardListBoxModel()
    		   .includeEmptyValue()
    		   .includeMatchingAs(
	               context instanceof Queue.Task
	                       ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
	                       : ACL.SYSTEM,
	               context,
	               StandardUsernameCredentials.class,
	               gitblitDomainRequirements(gitblitUri),
	               gitblitCredentialsMatcher()
       );
   }
   
   public static void setProxy(String host) {
	   Jenkins jenkins = Jenkins.getActiveInstance();
	   ProxyConfiguration proxyConfiguration = jenkins.proxy;
	   Proxy proxy = proxyConfiguration.createProxy(host);
	   
	   System.setProperty("http.proxyHost", proxy.address().toString());
   }
   
}
