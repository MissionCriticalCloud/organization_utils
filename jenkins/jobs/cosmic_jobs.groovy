import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCredentials
import jenkins.model.Jenkins
import org.kohsuke.github.GitHub

def DEFAULT_GIT_REPO_BRANCH = 'remotes/origin/pr/*/head'

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'sha1'
def CUSTOM_WORKSPACE_PARAM        = 'CUSTOM_WORKSPACE'

def WORKSPACE_VAR = '${WORKSPACE}'

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORGANIZATION_REPO_NAME           = 'organization_utils'
def ORGANIZATION_NAME                = 'MissionCriticalCloud'
def ORG_UTILS_GITHUB_REPOSITORY      = "${ORGANIZATION_NAME}/${ORGANIZATION_REPO_NAME}"
def COSMIC_GITHUB_REPOSITORY         = "${ORGANIZATION_NAME}/cosmic"
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_REPOSITORY_NAME_PARAM  = 'githubRepository'
def GITHUB_OAUTH2_CREDENTIAL_PARAM = 'mccdJenkinsOauth2'
def ARTEFACTS_TO_ARCHIVE_PARAM = 'artefactsToArchive'

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'

def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'

def DEFAULT_MARVIN_CONFIG_FILE = '/data/shared/marvin/mct-zone1-kvm1-kvm2.cfg'

def MAVEN_REPORTS = [
  '**/target/surefire-reports/*.xml',
  '**/target/failsafe-reports/*.xml'
]

def GENERIC_MAVEN_JOB_ARTIFACTS = [
  '**/target/*.war',
  '**/target/*.jar'
] + MAVEN_REPORTS

// TODO: update these paths
def COSMIC_MAVEN_BUILD_ARTEFACTS = [
  'client/target/cloud-client-ui-*.war',
  'client/target/utilities/',
  'client/target/conf/',
  'cloudstack-*.rpm',
  'tools/marvin/dist/Marvin-*.tar.gz',
  'setup/db/db/',
  'setup/db/create-*.sql',
  'setup/db/templates*.sql',
  'developer/developer-prefill.sql',
  'scripts/storage/secondary/',
  'test/integration/'
]

// dev folder is to play arround with jobs.
// Jobs defined there should never autmatically trigger
def FOLDERS = [
  'cosmic',
  'cosmic-dev'
]

def DEFAULT_EXECUTOR     = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

FOLDERS.each { folderName ->
  def seedJob                         = "${folderName}/seed-job"
  def updateCosmicRepoJob             = "${folderName}/update-cosmic-repo"
  def customWorkspaceMavenJob         = "${folderName}/custom-workspace-maven-job"
  def trackingRepoPullRequestBuild    = "${folderName}/tracking-repo-pull-request-build"
  def trackingRepoMasterBuild         = "${folderName}/tracking-repo-master-build"
  def prepareInfraForIntegrationTests = "${folderName}/prepare-infrastructure-for-integration-tests"


  def isDevFolder = folderName.endsWith('-dev')
  def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')
  def shellPrefix = isDevFolder ? 'bash -x' : ''

  folder(folderName)

  // seed job is meant to trigger when this file changes in git
  freeStyleJob(seedJob) {
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    label(DEFAULT_EXECUTOR)
    if(!isDevFolder) {
      scm {
        git {
          remote {
            github(ORG_UTILS_GITHUB_REPOSITORY, 'https' )
          }
          branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
          shallowClone(true)
          clean(true)
          configure { node ->
            node / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
              includedRegions 'jenkins/jobs/cosmic_jobs[.]groovy'
              excludedRegions ''
            }
          }
        }
      }
      triggers {
        githubPush()
      }
    }
    steps {
      // place a gradle build file that copies required dependencies into workspace
      shell([
        'echo "defaultTasks \'libs\'',
        'repositories {',
        '  jcenter()',
        '}',
        'configurations {',
        '  libs',
        '}',
        'dependencies {',
        '  libs \'org.kohsuke:github-api:1.70\'',
        '}',
        'task clean(type: Delete) {',
        '  delete \'lib\'',
        '}',
        'task libs(type: Copy) {',
        '  into \'lib\'',
        '  from configurations.libs',
        '}',
        'task wrapper(type: Wrapper) {',
        '  gradleVersion = \'2.2.1\'',
        '}" > build.gradle',
        '/usr/local/gradle/bin/gradle libs'
      ].join('\n'))
      dsl {
        if(!isDevFolder) {
          external('jenkins/jobs/cosmic_jobs.groovy')
        }
        additionalClasspath('lib/*')
      }
    }
  }

  // Build for master branch of tracking repo
  multiJob(trackingRepoMasterBuild) {
    displayName('Cosmic master build')
    parameters {
      stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
    }
    concurrentBuild()
    label(executorLabelMct)
    throttleConcurrentBuilds {
      maxPerNode(1)
    }
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    triggers {
      if (!isDevFolder) {
        cron('H/15 * * * *')
      }
    }
    scm {
      git {
        remote {
          github(COSMIC_GITHUB_REPOSITORY, 'ssh')
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
          name('origin')
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
        }
        branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(true)
      }
    }
    steps {
      if (!isDevFolder) {
        phase('Update tracking repository') {
          continuationCondition('SUCCESSFUL')
          phaseJob(updateCosmicRepoJob) {
            parameters {
              predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
              sameNode()
              gitRevision(false)
              killPhaseCondition('FAILURE')
            }
          }
        }
      }
      phase('Build code and prepare infrastructure for integrations tests') {
        phaseJob(customWorkspaceMavenJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
        phaseJob(prepareInfraForIntegrationTests) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
          }
        }
      }
    }
    publishers {
      archiveArtifacts {
        pattern(makePatternList(COSMIC_MAVEN_BUILD_ARTEFACTS))
        onlyIfSuccessful()
      }
      archiveJunit(makePatternList(MAVEN_REPORTS)) {
        retainLongStdout()
        testDataPublishers {
            publishTestStabilityData()
        }
      }
    }
  }

  // build for pull requests to tracking repo
  multiJob(trackingRepoPullRequestBuild) {
    displayName('Cosmic pull request build')
    parameters {
      stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'sha1', 'Branch to be built')
    }
    concurrentBuild()
    label(DEFAULT_EXECUTOR)
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    scm {
      git {
        remote {
          github(COSMIC_GITHUB_REPOSITORY, 'ssh')
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
          name('origin')
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
        }
        branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(true)
      }
    }
    triggers {
      if(!isDevFolder) {
        pullRequest {
          extensions {
            triggerPhrase('go build')
            permitAll()
            useGitHubHooks()
            commitStatus {
              context(DEFAULT_GITHUB_JOB_LABEL)
              startedStatus('building...')
              completedStatus('SUCCESS', 'All is well')
              completedStatus('FAILURE', 'Something went wrong. Investigate!')
            }
          }
        }
      }
    }
    steps {
      phase('Build Code') {
        phaseJob(customWorkspaceMavenJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
      }
    }
    publishers {
      archiveJunit(makePatternList(MAVEN_REPORTS)) {
        retainLongStdout()
        testDataPublishers {
            publishTestStabilityData()
        }
      }
    }
  }

  // Job that checks for changes in the sub-repos of the tracking repo
  // when there are changes it pulls them in and pushes a new commit
  freeStyleJob(updateCosmicRepoJob) {
    displayName('Cosmic tracking repository scheduled update')
    logRotator {
      numToKeep(100)
      artifactNumToKeep(10)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    scm {
      git {
        remote {
          github(COSMIC_GITHUB_REPOSITORY, 'ssh' )
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
        }
        branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
        shallowClone(false)
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(true)
      }
    }
    steps {
      shell([
        'git config --global user.email "int-mccd_jenkins@schubergphilis.com"',
        'git config --global user.name "mccd-jenkins"',
        'if [ -z "$(git status -su)" ]; then',
        '  echo "==> No submodule changed"',
        '  exit',
        'else',
        '  echo "==> Updating all submodules in remote repository"',
        '  git add --all',
        '  git commit -m "Update all submodules to latest HEAD"',
        '  git push origin HEAD:master',
        'fi'
      ].join('\n'))
    }
  }

  // generic Maven job that builds on a folder (instead of a git repo)
  // this job is meant to be called by another job that already checked out a maven project
  mavenJob(customWorkspaceMavenJob) {
    parameters {
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
      textParam(ARTEFACTS_TO_ARCHIVE_PARAM, '', 'The artefacts that should be archived when the build is successful')
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    environmentVariables {
      env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    goals('clean')
    goals('install')
    goals('-Pdeveloper')
    goals('-T4')
  }

  // Job that prepares the infrastructure for the cosmic integration tests
  freeStyleJob(prepareInfraForIntegrationTests) {
    concurrentBuild()
    label(executorLabelMct)
    throttleConcurrentBuilds {
      maxPerNode(1)
    }
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    steps {
      shell('rm -rf ./*')
      shell("${shellPrefix} /data/shared/ci/ci-prepare-infra.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
    }
  }

  // Pull request jobs for plugins
  getPluginRepositories(ORGANIZATION_NAME, DEFAULT_GITHUB_USER_NAME).each { cosmicRepo ->
    def targetBranch = injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM)
    def repoName = cosmicRepo.getName()
    def githubRepository = "${ORGANIZATION_NAME}/" + repoName
    def repoJobName =  "${folderName}/plugin-pull-request-build-${repoName}"

    // job to build cosmic a plugin
    multiJob(repoJobName) {
      displayName('Plugin pull request build: ' + repoName)
      parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, DEFAULT_GIT_REPO_BRANCH, 'Branch to be built')
      }
      concurrentBuild()
      label(DEFAULT_EXECUTOR)
      logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
      }
      wrappers {
        colorizeOutput('xterm')
        timestamps()
      }
      scm {
        git {
          remote {
            github(githubRepository, 'ssh')
            credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            name('origin')
            refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
          }
          branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
          clean(true)
        }
      }
      triggers {
        if(!isDevFolder) {
          pullRequest {
            extensions {
              triggerPhrase('go build')
              permitAll()
              useGitHubHooks()
              commitStatus {
                context(DEFAULT_GITHUB_JOB_LABEL)
                startedStatus('building...')
                completedStatus('SUCCESS', 'All is well')
                completedStatus('FAILURE', 'Something went wrong. Investigate!')
              }
            }
          }
        }
      }
      steps {
        phase('Build Code') {
          phaseJob(customWorkspaceMavenJob) {
            currentJobParameters(true)
            parameters {
              predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
              sameNode()
              gitRevision(false)
            }
          }
        }
      }
      if(repoName != 'cosmic-client') {
        publishers {
          archiveJunit(makePatternList(MAVEN_REPORTS)) {
            retainLongStdout(true)
            testDataPublishers {
                publishTestStabilityData()
            }
          }
        }
      }
    }
  }
}

def getPluginRepositories(githubOrganizatioName, githubUserName) {
  def githubCredentials = CredentialsProvider.lookupCredentials(
    StandardCredentials.class,
    Jenkins.instance
  )
  gitubUserCredentials = githubCredentials.find({ it.description.contains(githubUserName) && it.description.contains('OAuth2') })

  def github = GitHub.connect(githubUserName, gitubUserCredentials.secret.getPlainText())

  def organization = github.getOrganization(githubOrganizatioName);
  def repos = organization.listRepositories().toList();

  return repos.findAll { it.getName().startsWith('cosmic-') }
}

def injectJobVariable(variableName) {
  return '${' + variableName + '}'
}

def makePatternList(patterns) {
  return patterns.join(', ')
}

// used for testing in order to avoind calling github api all the time
class FakeRepo {

  private String name;

  public FakeRepo(String name) {
    this.name = name;
  }

  public String getName() { return name }
}
