package com.tsystems.sbs.gitblit;

import static hudson.model.Items.XSTREAM2;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.gitblit.models.RepositoryModel;
import com.gitblit.utils.RpcUtils;

import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceObserver.ProjectObserver;

/**
 * Navigates the repositories of the Gitblit instances, 
 * identifies the suitable ones 
 * and then synchronizes the organization.
 */
public class GitBlitSCMNavigator extends SCMNavigator {

	private static final int GIT_SUFFIX_LENGTH = 4;
	
	private final String gitblitUri;
	private transient String scanCredentialsId;
	private transient String checkoutCredentialsId;
	private String credentialsId;
	private String pattern = ".*";

	private String includes;
	private String excludes;
	
	/**
	 * Used for class and package name retrocompatibility.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        XSTREAM2.addCompatibilityAlias("com.tsystems.sbs.gitblitbranchsource.GitBlitSCMNavigator", GitBlitSCMNavigator.class);
    }
	
	/**
	 * Used for field retrocompatibility.
	 * @return this object.
	 */
	private Object readResolve() {
		if (checkoutCredentialsId != null) {
			credentialsId = checkoutCredentialsId;
		} else if (scanCredentialsId != null) {
			credentialsId = scanCredentialsId;
		}
		
		return this;
	}

	/**
	 * Constructs a GitBlitSCMNavigator which scans Gitblit repositories.
	 * @param gitblitUri The Gitblit instance uri.
	 * @param credentialsId Credentials to scan repositories and (Git) check out.
	 */
	@DataBoundConstructor
	public GitBlitSCMNavigator(String gitblitUri, String credentialsId){
		this.gitblitUri = Util.fixEmptyAndTrim(gitblitUri);
		this.credentialsId = credentialsId;
	}

	public String getIncludes() {
		return includes != null ? includes : DescriptorImpl.DEFAULT_INCLUDES;
	}

	@DataBoundSetter
	public void setIncludes(String includes) {
		this.includes = includes.equals(DescriptorImpl.DEFAULT_INCLUDES) ? null : includes;
	}

	public String getExcludes() {
		return excludes != null ? excludes : DescriptorImpl.DEFAULT_EXCLUDES;
	}

	@DataBoundSetter
	public void setExcludes(String excludes) {
		this.excludes = excludes.equals(DescriptorImpl.DEFAULT_EXCLUDES) ? null : excludes;
	}

	public String getGitblitUri() {
		return gitblitUri;
	}

	public String getPattern() {
		return pattern;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@DataBoundSetter
	public void setPattern(String pattern) {
		//		Pattern.compile(pattern);
		this.pattern = pattern;
	}

	public boolean isGitblitUriSelectable() {
		return !GitBlitConfiguration.get().getEndpoints().isEmpty();
	}
	
	@Override
	protected String id() {
		// Generate the ID of the thing being navigated.
		// Typically this will, at a minimum consist of the URL of the remote server
		// For GitHub it would probably also include the GitHub Organization being navigated
		// For BitBucket it could include the owning team as well as the project (if navigation is scoped to
		// a single project within a team) or just the owning team (if navigation is scoped to all repositories
		// in a team)
		//
		// See the Javadoc for more details.
		return gitblitUri;//TODO: set a more complex ID
	}
	

	//This method is called when the "Folder Computation" action is triggered
	@Override
	public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
		TaskListener listener = observer.getListener();
		PrintStream logger = listener.getLogger();

		if (gitblitUri != null) {
			StandardUsernamePasswordCredentials credentials = 
					(StandardUsernamePasswordCredentials) Connector.lookupCredentials(
							(Item)observer.getContext(), 
							gitblitUri, 
							credentialsId);
			String user = null;
			String password = "";
			if (credentials != null) {
				user = credentials.getUsername();
				password = credentials.getPassword().getPlainText();
				
			}
		
			logger.println("Connecting to Gitblit at " + gitblitUri);
			//Connect to GitBlit and scan the repos for Jenkinsfile's
			Map<String, RepositoryModel> response = RpcUtils.getRepositories(gitblitUri, user, password.toCharArray());
		
			Iterator<Entry<String, RepositoryModel>> repoIterator = response.entrySet().iterator();
			while(repoIterator.hasNext()) {
				Entry<String, RepositoryModel> repoEntry = repoIterator.next();
				
				String repoUrl = repoEntry.getKey();
				logger.println("Found repository: " + repoUrl);
		
				RepositoryModel repository = repoEntry.getValue();
				String repoName = repository.name;
				repoName = repoName.substring(0,repoName.length() - GIT_SUFFIX_LENGTH);//remove the .git suffix
		
				//Filter the projects and add them only if they match the pattern
				if(Pattern.compile(pattern).matcher(repoName).matches()) {
					repoName = repoName.replace('/', '-');
		
					logger.println("Adding repository: " + repoEntry + " with name " + repoName);
					ProjectObserver projectObserver = observer.observe(repoName);
		
					GitBlitSCMSource gitblitSource = 
							new GitBlitSCMSource(getId() + repoName, 
									gitblitUri, 
									credentialsId, 
									repoUrl, 
									getIncludes(), 
									getExcludes());
		
					projectObserver.addSource(gitblitSource);
					projectObserver.complete();
				} else {
					logger.println("Ignoring repo: " + repoEntry + " with name " + repoName);
				}
			}
		} else {
			logger.println("No Gitblit instance has been specified. A Gitblit server must be chosen "
					+ "from the ones specified at \"Manage Jenkins -> Configure System\" ");
		}

		return;
	}

	/**
	 * Our descriptor.
	 */
	@Symbol("gitblit")
	@Extension
	public static class DescriptorImpl extends SCMNavigatorDescriptor {

		public static final String DEFAULT_INCLUDES = "*";
        public static final String DEFAULT_EXCLUDES = "";

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPronoun() {
			return "Organization";
		}

		@Override
		public String getDisplayName() {
			return "GitBlit Organization";
		}

		@Override
		public String getDescription() {
			return "Scans the specified GitBlit servers for repositories containing a Jenkinsfile.";
		}

		@Override
		public SCMNavigator newInstance(String name) {
			return new GitBlitSCMNavigator(name, "credentials");
		}

		/**
		 * Method used by the UI to populate the credentialsId element
		 * @param context The Jenkins context.
		 * @param gitblitUri The Gitblit instance's URI.
		 * @return The ListBoxModel which fills the UI element.
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String gitblitUri) {
			return Connector.listCredentials(context, gitblitUri);
		}

		/**
		 * Method used by the UI to populate the gitblitUri element
		 * @return The ListBoxModel which fills the UI element.
		 */
		public ListBoxModel doFillGitblitUriItems() {
			ListBoxModel result = new ListBoxModel();
			for (Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
				result.add(e.getName() == null ? e.getGitblitUri() : e.getName(), e.getGitblitUri());
			}
			return result;
		}

		public boolean isGitblitUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}

	}
}
