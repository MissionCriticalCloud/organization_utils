import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials
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

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'

def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'

def MAVEN_REPORTS = [
  '**/target/surefire-reports/*.xml',
  '**/target/failsafe-reports/*.xml'
]

def MAVEN_JOB_ARTIFACTS = [
  '**/target/*.war',
  '**/target/*.jar'
] + MAVEN_REPORTS

// dev folder is to play arround with jobs.
// Jobs defined there should never autmatically trigger
def FOLDERS = [
  'cosmic',
  'cosmic-dev'
]

def DEFAULT_EXECUTOR     = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

FOLDERS.each { folderName ->
  def seedJob                      = "${folderName}/seed-job"
  def updateCosmicRepoJob          = "${folderName}/update-cosmic-repo"
  def customWorkspaceMavenJob      = "${folderName}/custom-workspace-maven-job"
  def trackingRepoPullRequestBuild = "${folderName}/tracking-repo-pull-request-build"
  def trackingRepoMasterBuild      = "${folderName}/tracking-repo-master-build"


  def isDevFolder = folderName.endsWith('-dev')
  def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')

  folder(folderName)

  // seed job is meant to trigger when this file changes in git
  freeStyleJob(seedJob) {
    logRotator {
      numToKeep(10)
      artifactNumToKeep(10)
    }
    label(DEFAULT_EXECUTOR)
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
      if(!isDevFolder) {
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
        '}" > build.gradle'
      ].join('\n'))
      // bundle dependencies
      gradle {
        gradleName('default gradle')
        tasks('libs')
      }
      dsl {
        external('jenkins/jobs/cosmic_jobs.groovy')
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
      numToKeep(5)
      artifactNumToKeep(5)
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
        branch('${' + DEFAULT_GIT_REPO_BRANCH_PARAM + '}')
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(true)
      }
    }
    steps {
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
      phase('Build Code') {
        phaseJob(customWorkspaceMavenJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            predefinedProp(GITHUB_REPOSITORY_NAME_PARAM, COSMIC_GITHUB_REPOSITORY)
            sameNode()
            gitRevision(false)
          }
        }
      }
      copyArtifacts(customWorkspaceMavenJob) {
        includePatterns(MAVEN_REPORTS.join(', '))
        fingerprintArtifacts(true)
        buildSelector {
          multiJobBuild()
        }
      }
    }
    publishers {
      archiveJunit(MAVEN_REPORTS.join(', ')) {
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
      numToKeep(5)
      artifactNumToKeep(5)
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
        branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
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
            predefinedProp(GITHUB_REPOSITORY_NAME_PARAM, COSMIC_GITHUB_REPOSITORY)
            sameNode()
            gitRevision(false)
          }
        }
      }
      copyArtifacts(customWorkspaceMavenJob) {
        includePatterns(MAVEN_REPORTS.join(', '))
        fingerprintArtifacts(true)
        buildSelector {
          multiJobBuild()
        }
      }
    }
    publishers {
      archiveJunit(MAVEN_REPORTS.join(', ')) {
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
      stringParam(GITHUB_REPOSITORY_NAME_PARAM, '', 'The GitHub repository to build')
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    environmentVariables {
      env(GITHUB_OAUTH2_TOKEN_ENV_VAR, '${' + GITHUB_OAUTH2_CREDENTIAL_PARAM + '}')
    }
    customWorkspace('${' + CUSTOM_WORKSPACE_PARAM + '}')
    logRotator {
      numToKeep(50)
      artifactNumToKeep(50)
    }
    concurrentBuild()
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    goals('clean')
    goals('install')
    goals('-P developer')
  }

  getPluginRepositories(ORGANIZATION_NAME, DEFAULT_GITHUB_USER_NAME).each { cosmicRepo ->
    def targetBranch = '${' + DEFAULT_GIT_REPO_BRANCH_PARAM + '}'
    def repoName = cosmicRepo.getName()
    def githubRepository = "${ORGANIZATION_NAME}/" + repoName
    def repoJobName =  "${folderName}/plugin-pull-request-build-${repoName}"

    // job to build cosmic a plugin
    multiJob(repoJobName) {
      displayName('Plugin pull request build: ' + repoName)
      parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'sha1', 'Branch to be built')
      }
      concurrentBuild()
      label(DEFAULT_EXECUTOR)
      logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
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
          branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
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
              predefinedProp(GITHUB_REPOSITORY_NAME_PARAM, githubRepository)
              sameNode()
              gitRevision(false)
            }
          }
        }
        copyArtifacts(customWorkspaceMavenJob) {
          includePatterns(MAVEN_REPORTS.join(', '))
          fingerprintArtifacts(true)
          optional(true)
          buildSelector {
            multiJobBuild()
          }
        }
      }
      publishers {
        archiveJunit(MAVEN_REPORTS.join(', ')) {
          retainLongStdout(true)
          testDataPublishers {
              publishTestStabilityData()
          }
        }
      }
    }
  }
}

def getPluginRepositories(githubOrganizatioName, githubUserName) {
  def githubCredentials = CredentialsProvider.lookupCredentials(
    StandardUsernameCredentials.class,
    Jenkins.instance
  )
  gitubUserCredentials = githubCredentials.find({ it.username == githubUserName })

  def github = GitHub.connectUsingPassword(gitubUserCredentials.username, gitubUserCredentials.password.getPlainText())

  def organization = github.getOrganization(githubOrganizatioName);
  def repos = organization.listRepositories().toList();

  return repos.findAll { it.getName().startsWith('cosmic-') }
}
