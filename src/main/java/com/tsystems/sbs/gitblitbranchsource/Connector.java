package com.tsystems.sbs.gitblitbranchsource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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

import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class Connector {
	
	public static final String LIST_BRANCHES = "rpc/?req=LIST_BRANCHES";
	public static final String LIST_REPOSITORIES = "rpc/?req=LIST_REPOSITORIES";
	
	public static JSONObject connect(String gitblitUri, String rpcFunction,final String username, final String password) throws IOException {
		URL functionUrl = new URL(gitblitUri + (gitblitUri.endsWith("/") ? "" : "/") + rpcFunction);//Append a '/' if the host URL doesn't end with '/'
		
		HttpURLConnection connection = (HttpURLConnection) functionUrl.openConnection(getProxy(functionUrl.getHost()));
		
		if (username != null && password != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username,password.toCharArray());
				}
			});
		}
			
		InputStream stream = connection.getInputStream();
		InputStreamReader isReader = new InputStreamReader(stream, "UTF-8");
		
		//put outputStream into a string
		BufferedReader br = new BufferedReader(isReader);
		
		try {
			StringBuilder jsonString = new StringBuilder();
			String line;
			while((line = br.readLine()) != null)
				jsonString.append(line);
			
			JSONObject response = JSONObject.fromObject(jsonString.toString());
			
			return response;
		} finally {
			try { stream.close(); } catch (IOException e) { e.printStackTrace(); }
			try { isReader.close(); } catch (IOException e) { e.printStackTrace(); }
			try { br.close(); } catch (IOException e) {e.printStackTrace(); }
		}
		
	}
	
	/**
    * Uses proxy if configured on pluginManager/advanced page
    *
    * @param host Gitblit's hostname to build proxy to
    *
    * @return proxy to use it in connector. Should not be null as it can lead to unexpected behaviour
    */
   @Nonnull
   private static Proxy getProxy(@Nonnull String host) {
       Jenkins jenkins = Jenkins.getActiveInstance();

       if (jenkins.proxy == null) {
           return Proxy.NO_PROXY;
       } else {
           return jenkins.proxy.createProxy(host);
       }
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
   
}
