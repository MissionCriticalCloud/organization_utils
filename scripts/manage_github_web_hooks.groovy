import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.kohsuke.github.GitHub
import org.kohsuke.github.GHEvent
import jenkins.model.Jenkins
import hudson.model.ParametersAction
import java.util.Arrays

def parameters = build?.actions.find{ it instanceof ParametersAction }?.parameters
def githubOrganizatioName = parameters.find({ it.name == 'githubOrganizatioName' }).value
def githubUserName = parameters.find({ it.name == 'githubUserName' }).value
def webHookSecretKeyword = parameters.find({ it.name == 'webHookSecretKeyword' }).value
def repoEvents = parameters.find({ it.name == 'repoEvents' }).value

def webHookSecrets = CredentialsProvider.lookupCredentials(
  StringCredentials.class,
  Jenkins.instance
)
def webHookSecret = webHookSecrets.find({ it.description.contains webHookSecretKeyword })
def webHookUrl = webHookSecret.getSecret().getPlainText()

def githubCredentials = CredentialsProvider.lookupCredentials(
  StandardUsernameCredentials.class,
  Jenkins.instance
)
gitubUserCredentials = githubCredentials.find({ it.username == githubUserName })

def github = GitHub.connectUsingPassword(gitubUserCredentials.username, gitubUserCredentials.password.getPlainText())

def organization = github.getOrganization(githubOrganizatioName);
def repos = organization.listRepositories().toList();
def githubEvents = Arrays.asList(repoEvents.split('\\s+')).collect {
  GHEvent.valueOf(it)
}

def hookConfig = [content_type: "json", insecure_ssl: "0"]

repos.each { repo ->
  println("Checking ${repo}");
  def hooks = repo.getHooks();
  if(hooks.isEmpty() || hooks.find { h ->  h.getConfig().containsKey('url') && h.getConfig().get('url').equals(webHookUrl) } == null) {
    println("Repo ${repo} does not have the ${webHookSecretKeyword} hook");
    def config = hookConfig.clone()
    config.put("url", new URL(webHookUrl).toExternalForm())
    repo.createHook("web", config, githubEvents, true);
  }
}
