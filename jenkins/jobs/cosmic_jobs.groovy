def DEFAULT_GIT_REPO_BRANCH = 'remotes/origin/pr/*/head'

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'sha1'
def CUSTOM_WORKSPACE_PARAM        = 'CUSTOM_WORKSPACE'

def WORKSPACE_VAR = '${WORKSPACE}'

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORG_UTILS_GITHUB_REPOSITORY      = 'MissionCriticalCloud/organization_utils'
def COSMIC_GITHUB_REPOSITORY         = 'MissionCriticalCloud/cosmic'
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_REPOSITORY_NAME_PARAM  = 'githubRepository'
def GITHUB_OAUTH2_CREDENTIAL_PARAM = 'mccdJenkinsOauth2'

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'

def FOLDERS = [
  'cosmic',
  'cosmic-dev'
]

def DEFAULT_EXECUTOR     = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

FOLDERS.each { folderName ->
  def seedJob             = "${folderName}/seed-job"
  def updateCosmicRepoJob = "${folderName}/902-update-cosmic-repo"
  def genericMavenJob     = "${folderName}/903-generic-maven-job"
  def fullBuildJobName    = "${folderName}/001-full-build"


  def isDevFolder = folderName.endsWith('-dev')
  def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')

  folder(folderName)

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
      dsl {
        external('jenkins/jobs/cosmic_jobs.groovy')
      }
    }
  }

  freeStyleJob(updateCosmicRepoJob) {
    logRotator {
      numToKeep(100)
      artifactNumToKeep(10)
    }
    label(DEFAULT_EXECUTOR)
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
    triggers {
      if (!isDevFolder) {
        cron('H/15 * * * *')
      }
    }
    steps {
      shell([
        'git config --global user.email "int-mccd_jenkins@schubergphilis.com"',
        'git config --global user.name "mccd-jenkins"',
        'if [ -z "$(git status -su)" ]; then',
        '  echo "==> No submodule changed"',
        'else',
        '  echo "==> Updating all submodules in remote repository"',
        '  git add --all',
        '  git commit -m "Update all submodules to latest HEAD"',
        '  git push origin HEAD:master',
        'fi'
      ].join('\n'))
    }
  }

  mavenJob(genericMavenJob) {
    parameters {
      stringParam(GITHUB_REPOSITORY_NAME_PARAM, '', 'The GitHub repository to build')
      stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, DEFAULT_GIT_REPO_BRANCH, 'Branch to be checked out and built')
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    environmentVariables {
      env(GITHUB_OAUTH2_CREDENTIAL_PARAM, "${GITHUB_REPOSITORY_NAME_PARAM}")
    }
    customWorkspace('${' + CUSTOM_WORKSPACE_PARAM + '}')
    logRotator {
      numToKeep(50)
      artifactNumToKeep(50)
    }
    concurrentBuild()
    label(executorLabelMct)
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    scm {
      git {
        remote {
          url('git@github.com:${' + GITHUB_REPOSITORY_NAME_PARAM + '}')
          name('origin')
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
        }
        branch('${' + DEFAULT_GIT_REPO_BRANCH_PARAM + '}')
        shallowClone(true)
        clean(true)
      }
    }
    goals('clean')
    goals('install')
    goals('-P developer')
    publishers {
      archiveJunit('**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml') {
        retainLongStdout()
        testDataPublishers {
            publishTestStabilityData()
        }
      }
    }
  }

  multiJob(fullBuildJobName) {
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
      phase('Checkout Code, Build and Package') {
        phaseJob(genericMavenJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            predefinedProp(GITHUB_REPOSITORY_NAME_PARAM, COSMIC_GITHUB_REPOSITORY)
            predefinedProp(DEFAULT_GIT_REPO_BRANCH_PARAM, DEFAULT_GITHUB_REPOSITORY_BRANCH)
            sameNode()
            gitRevision(false)
          }
        }
      }
    }
  }
}
