package com.tsystems.sbs.gitblitbranchsource;

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
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceObserver.ProjectObserver;

public class GitBlitSCMNavigator extends SCMNavigator {

	private final String gitblitUri;
	private final String scanCredentialsId;
	private final String checkoutCredentialsId;
	private String pattern = ".*";

	private String includes;
	private String excludes;

	@DataBoundConstructor
	public GitBlitSCMNavigator (String gitblitUri, String scanCredentialsId, String checkoutCredentialsId){
		this.gitblitUri = Util.fixEmptyAndTrim(gitblitUri);
		this.scanCredentialsId = scanCredentialsId;
		this.checkoutCredentialsId = checkoutCredentialsId;
	}

	public String getIncludes() {
		return includes != null ? includes : DescriptorImpl.defaultIncludes;
	}

	@DataBoundSetter
	public void setIncludes(String includes) {
		this.includes = includes.equals(DescriptorImpl.defaultIncludes) ? null : includes;
	}

	public String getExcludes() {
		return excludes != null ? excludes : DescriptorImpl.defaultExcludes;
	}

	@DataBoundSetter
	public void setExcludes(String excludes) {
		this.excludes = excludes.equals(DescriptorImpl.defaultExcludes) ? null : excludes;
	}

	public String getGitblitUri() {
		return gitblitUri;
	}

	public String getPattern() {
		return pattern;
	}

	public String getScanCredentialsId() {
		return scanCredentialsId;
	}

	public String getCheckoutCredentialsId() {
		return checkoutCredentialsId;
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
			StandardUsernamePasswordCredentials credentials = (StandardUsernamePasswordCredentials) Connector.lookupScanCredentials((Item)observer.getContext(), gitblitUri, scanCredentialsId);
			String user = null;
			String password = "";
			if (credentials != null) {
				user = credentials.getUsername();
				password = credentials.getPassword().getPlainText();
				
			}
		
			//TODO: configure proxy?
				
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
				repoName = repoName.substring(0,repoName.length() - 4);//remove the .git suffix
		
				//Filter the projects and add them only if they match the pattern
				if(Pattern.compile(pattern).matcher(repoName).matches()) {
					repoName = repoName.replace('/', '-');
		
					logger.println("Adding repository: " + repoEntry + " with name " + repoName);
					ProjectObserver projectObserver = observer.observe(repoName);
		
					GitBlitSCMSource gitblitSource = new GitBlitSCMSource(getId() + repoName, gitblitUri, checkoutCredentialsId, scanCredentialsId, repoUrl, getIncludes(), getExcludes());
		
					projectObserver.addSource(gitblitSource);
					projectObserver.complete();
				} else {
					logger.println("Ignoring repo: " + repoEntry + " with name " + repoName);
				}
			}
		} else {
			logger.println("No Gitblit instance has been specified. A Gitblit server must be chosen from the ones specified at \"Manage Jenkins -> Configure System\" ");
		}

		return;
	}

	@Symbol("gitblit")
	@Extension
	public static class DescriptorImpl extends SCMNavigatorDescriptor {

		public static final String defaultIncludes = "*";
        public static final String defaultExcludes = "";
		public static final String SAME = GitBlitSCMSource.DescriptorImpl.SAME;

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
			return "Scans a GitBlit organization for all repositories with a Jenkinsfile.";
		}

		@Override
		public SCMNavigator newInstance(String name) {
			return new GitBlitSCMNavigator(name, "", GitBlitSCMSource.DescriptorImpl.SAME);
		}

		public ListBoxModel doFillScanCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String gitblitUri) {
			return Connector.listScanCredentials(context, gitblitUri);
		}

		public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String gitblitUri) {
			return Connector.listCheckoutCredentials(context, gitblitUri);
		}

		public ListBoxModel doFillGitblitUriItems() {
			ListBoxModel result = new ListBoxModel();
			for (Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
				result.add(e.getName() == null ? e.getApiUri() : e.getName(), e.getApiUri());
			}
			return result;
		}

		public boolean isGitblitUriSelectable() {
			return !GitBlitConfiguration.get().getEndpoints().isEmpty();
		}

	}
}
