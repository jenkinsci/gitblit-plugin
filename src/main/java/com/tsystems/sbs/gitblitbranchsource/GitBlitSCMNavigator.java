package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMNavigatorEvent;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceObserver.ProjectObserver;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GitBlitSCMNavigator extends SCMNavigator {

	private final String apiUri;
	private String pattern = ".*";
	
	private String includes;
	private String excludes;
	
	@DataBoundConstructor
	public GitBlitSCMNavigator (String apiUri){
		this.apiUri = Util.fixEmptyAndTrim(apiUri);
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
	
	public String getApiUri() {
		return apiUri;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	@DataBoundSetter
	public void setPattern(String pattern) {
//		Pattern.compile(pattern);
		this.pattern = pattern;
	}
	
	public boolean isApiUriSelectable() {
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
		return apiUri;//TODO: set a more complex ID
	}

	
	//This method is called when the "Folder Computation" action is triggered
	@Override
	public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
		TaskListener listener = observer.getListener();
		PrintStream logger = listener.getLogger();
		
		// TODO: Input data validation
		
		
		//Connect to GitBlit and scan the repos for Jenkinsfile's
//		String listRepositoriesSuffix = apiUri.endsWith("/") ? "rpc/?req=LIST_REPOSITORIES" : "/rpc/?req=LIST_REPOSITORIES";//Ensure that it ends with "/"
//		logger.println("Connecting to GitBlit api: " + apiUri + listRepositoriesSuffix + ":");
		JSONObject response = Connector.listRepositories(apiUri);
		logger.println("Response: "+response.toString(4));
		
		JSONArray repoURLs = response.names();
		
		for (int i=0;i<repoURLs.size();i++) {
			checkInterrupt();
			
			String repoURL = repoURLs.getString(i);//Get repository URL
			JSONObject repository = (JSONObject) response.get(repoURL);
			
			String repoName = repository.getString("name");
//			repoName = repoName.substring(0, repoName.length() - 3);//remove the .git suffix
			
//			String repoName = repoURL.replaceAll(".*\\/(\\S*).git","$1");//Get repository name
			
			//Filter the projects and add them only if they match the pattern
			if(Pattern.compile(pattern).matcher(repoName).matches()) {
				logger.println("Adding repo: " + repoURL + " with name " + repoName);
				ProjectObserver projectObserver = observer.observe(repoName);
				
				GitBlitSCMSource gitblitSource = new GitBlitSCMSource(getId()+repoName,apiUri,repoURL);
				gitblitSource.setIncludes(getIncludes());
				gitblitSource.setExcludes(getExcludes());
				
				projectObserver.addSource(gitblitSource);
				projectObserver.complete();
			} else {
				logger.println("Ignoring repo: " + repoURL + " with name " + repoName);
			}
			
		}
		
		return;
	}
	
	@Override
	public List<Action> retrieveActions(SCMNavigatorOwner owner, SCMNavigatorEvent event, TaskListener listener) throws IOException, InterruptedException {
		List<Action> result = new ArrayList<>();
		// If your SCM provides support for metadata at the "SCMNavigator" level
		// then you probably want to return at least a "jenkins.branch.MetadataAction"
		// from this method. The listener can be used to log the interactions
		// with the backing source control system.
		//
		// When you implement event support, if you have events when populating the
		// action (if that will avoid extra network calls and give the same result)
		return result;
	}
	
	@Symbol("gitblit")
    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

		private static final String defaultIncludes = GitBlitSCMSource.DescriptorImpl.defaultIncludes;
		private static final String defaultExcludes = GitBlitSCMSource.DescriptorImpl.defaultExcludes;
		
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
        	return new GitBlitSCMNavigator(name);
        }

        public ListBoxModel doFillApiUriItems() {
            ListBoxModel result = new ListBoxModel();
            for (Endpoint e : GitBlitConfiguration.get().getEndpoints()) {
                result.add(e.getName() == null ? e.getApiUri() : e.getName(), e.getApiUri());
            }
            return result;
        }

        public boolean isApiUriSelectable() {
            return !GitBlitConfiguration.get().getEndpoints().isEmpty();
        }

	}
}
