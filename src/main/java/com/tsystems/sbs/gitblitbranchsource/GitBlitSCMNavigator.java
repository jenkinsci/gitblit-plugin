package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceObserver.ProjectObserver;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GitBlitSCMNavigator extends SCMNavigator {

	private final String apiUri;
	private String pattern = ".*";
	
	@DataBoundConstructor
	public GitBlitSCMNavigator (String apiUri){
		this.apiUri = apiUri;
	}
	
	public String getApiUri() {
		return apiUri;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	public void setPattern(String pattern) {
//		Pattern.compile(pattern);
		this.pattern = pattern;
	}
	
	@Override
	protected String id() {
		return "GitBlit";
	}

	@Override
	public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
		TaskListener listener = observer.getListener();
		PrintStream logger = listener.getLogger();
		
		// Input data validation
		
		//Connect to GitBlit and scan the repos for Jenkinsfile's
		logger.println("Connecting to GitBlit api: "+apiUri+":");
		JSONObject response = Connector.connect(apiUri);
		logger.println("Response: "+response.toString(4));
		
		JSONArray repoURLs = response.names();
		
		for (int i=0;i<repoURLs.size();i++) {
			String repoURL = repoURLs.getString(i);
			logger.println("Adding repo: "+repoURLs.get(i));
			ProjectObserver projectObserver = observer.observe(repoURLs.getString(i));
			
			projectObserver.addSource(new GitBlitSCMSource(getId(),repoURL));
			projectObserver.complete();
		}
		
		return;
	}
	
	@Symbol("gitblit")
    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

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
//        	super.createCategories();
            return "Scans a GitBlit organization for all repositories with a Jenkinsfile.";
        }
        
        @Override
        public SCMNavigator newInstance(String name) {
        	return new GitBlitSCMNavigator("");
//            return new GitHubSCMNavigator("", name, "", GitHubSCMSource.DescriptorImpl.SAME);
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
