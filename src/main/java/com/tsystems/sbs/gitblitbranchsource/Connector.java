package com.tsystems.sbs.gitblitbranchsource;

import java.net.Proxy;
import java.util.List;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.gitclient.GitClient;

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
	
	private Connector() {
		throw new IllegalAccessError("Utility class");
	}
	
	static List<DomainRequirement> gitblitDomainRequirements(String apiUri) {
		return URIRequirementBuilder.fromUri(apiUri).build();
	}
   
   /**
    * Resolves the specified scan credentials in the specified context for use against the specified API endpoint.
    *
    * @param context           the context.
    * @param apiUri            the API endpoint.
    * @param scanCredentialsId the credentials to resolve.
    * @return the {@link StandardCredentials} or {@code null}
    */
   @CheckForNull
   public static StandardCredentials lookupScanCredentials(@CheckForNull Item context,
                                                           @CheckForNull String apiUri,
                                                           @CheckForNull String scanCredentialsId) {
       if (Util.fixEmpty(scanCredentialsId) == null) {
           return null;
       } else {
           return CredentialsMatchers.firstOrNull(
               CredentialsProvider.lookupCredentials(
                   StandardUsernameCredentials.class,
                   context,
                   context instanceof Queue.Task
                           ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                           : ACL.SYSTEM,
                   gitblitDomainRequirements(apiUri)
               ),
               CredentialsMatchers.allOf(CredentialsMatchers.withId(scanCredentialsId), gitblitScanCredentialsMatcher())
           );
       }
   }
   
   private static CredentialsMatcher gitblitScanCredentialsMatcher() {
       // TODO OAuth credentials
       return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
   }
   
   /**
    * Populates a {@link ListBoxModel} with the scan credentials appropriate for the supplied context against the
    * supplied API endpoint.
    *
    * @param context the context.
    * @param apiUri  the api endpoint.
    * @return a {@link ListBoxModel}.
    */
   public static ListBoxModel listScanCredentials(Item context, String apiUri) {
       return new StandardListBoxModel()
               .includeEmptyValue()
               .includeMatchingAs(
                       context instanceof Queue.Task
                               ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                               : ACL.SYSTEM,
                       context,
                       StandardUsernameCredentials.class,
                       gitblitDomainRequirements(apiUri),
                       gitblitScanCredentialsMatcher()
               );
   }
   
   /**
    * Populates a {@link ListBoxModel} with the checkout credentials appropriate for the supplied context against the
    * supplied API endpoint.
    *
    * @param context the context.
    * @param apiUri  the api endpoint.
    * @return a {@link ListBoxModel}.
    */
   public static ListBoxModel listCheckoutCredentials( Item context, String apiUri) {
       StandardListBoxModel result = new StandardListBoxModel();
       result.includeEmptyValue();
       result.add("- same as scan credentials -", "SAME");
       result.add("- anonymous -", "ANONYMOUS");
       return result.includeMatchingAs(
               context instanceof Queue.Task
                       ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                       : ACL.SYSTEM,
               context,
               StandardUsernameCredentials.class,
               gitblitDomainRequirements(apiUri),
               GitClient.CREDENTIALS_MATCHER
       );
   }
   
   public static void setProxy(String host) {
	   Jenkins jenkins = Jenkins.getActiveInstance();
	   ProxyConfiguration proxyConfiguration = jenkins.proxy;
	   Proxy proxy = proxyConfiguration.createProxy(host);
	   
	   System.setProperty("http.proxyHost", proxy.address().toString());
   }
   
}
