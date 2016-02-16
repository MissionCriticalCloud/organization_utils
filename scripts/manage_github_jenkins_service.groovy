import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCredentials
import org.kohsuke.github.GitHub
import org.kohsuke.github.GHEvent
import jenkins.model.Jenkins
import hudson.model.ParametersAction

def parameters = build?.actions.find{ it instanceof ParametersAction }?.parameters
def githubOrganizatioName = parameters.find({ it.name == 'githubOrganizatioName' }).value
def githubUserName = parameters.find({ it.name == 'githubUserName' }).value
def jenkinsUrl = parameters.find({ it.name == 'jenkinsUrl' }).value

def SERVICE_NAME = 'jenkins'

def githubCredentials = CredentialsProvider.lookupCredentials(
  StandardCredentials.class,
  Jenkins.instance
)
gitubUserCredentials = githubCredentials.find({ it.description.contains(githubUserName) && it.description.contains('OAuth2') })

def github = GitHub.connect(githubUserName, gitubUserCredentials.secret.getPlainText())

def organization = github.getOrganization(githubOrganizatioName);
def repos = organization.listRepositories().toList();

repos.each { repo ->
  println("Checking ${repo}");
  def hooks = repo.getHooks();
  if(hooks.isEmpty() || hooks.find { h ->  h.getName() == SERVICE_NAME } == null) {
    println("Repo ${repo} does not have the jenkins hook");
    repo.createHook(SERVICE_NAME, [jenkins_hook_url: new URL(jenkinsUrl + '/github-webhook/').toExternalForm()], [GHEvent.PUSH], true);
  }
}
