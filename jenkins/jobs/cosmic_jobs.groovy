def COSMIC_FOLDER_NAME = 'cosmic'

def DEFAULT_GIT_REPO_BRANCH = 'remotes/origin/pr/*/head'

def GENERIC_MAVEN_JOB      = "${COSMIC_FOLDER_NAME}/generic-maven-job"
def UPDATE_COSMIC_REPO_JOB = "${COSMIC_FOLDER_NAME}/update-cosmic-repo"
def SEED_JOB               = "${COSMIC_FOLDER_NAME}/seed-job"

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORG_UTILS_GITHUB_REPOSITORY      = 'MissionCriticalCloud/organization_utils'
def COSMIC_GITHUB_REPOSITORY         = 'MissionCriticalCloud/cosmic'
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_REPOSITORY_NAME_PARAM  = 'githubRepository'
def GITHUB_OAUTH2_CREDENTIAL_PARAM = 'mccdJenkinsOauth2'

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'

folder(COSMIC_FOLDER_NAME)

freeStyleJob(SEED_JOB) {
  logRotator {
    numToKeep(10)
    artifactNumToKeep(10)
  }
  label('executor')
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
          includedRegions 'jenkins/jobs/repo_jobs[.]groovy'
          excludedRegions ''
        }
      }
    }
  }
  triggers {
    githubPush()
  }
  steps {
    dsl {
      external('jenkins/jobs/cosmic_jobs.groovy')
    }
  }
}

freeStyleJob(UPDATE_COSMIC_REPO_JOB) {
  logRotator {
    numToKeep(100)
    artifactNumToKeep(10)
  }
  label('executor')
  wrappers {
    colorizeOutput('xterm')
    timestamps()
  }
  scm {
    git {
      remote {
        github(COSMIC_GITHUB_REPOSITORY, 'https' )
      }
      branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
      shallowClone(false)
      clean(true)
      recursiveSubmodules(true)
      trackingSubmodules(true)
    }
  }
  triggers {
    scm('H/15 * * * *')
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

mavenJob(GENERIC_MAVEN_JOB) {
  parameters {
    stringParam(GITHUB_REPOSITORY_NAME_PARAM, '', 'The GitHub repository to build')
    stringParam('sha1', DEFAULT_GIT_REPO_BRANCH, 'Branch to be checked out and built')
    credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
      type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
      required()
      description('mccd jenkins OAuth2 token credential')
    }
  }
  environmentVariables {
    env(GITHUB_OAUTH2_CREDENTIAL_PARAM, "${GITHUB_REPOSITORY_NAME_PARAM}")
  }
  logRotator {
    numToKeep(50)
    artifactNumToKeep(50)
  }
  concurrentBuild()
  label('executor')
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
      }
      branch('${sha1}')
      shallowClone(true)
      clean(true)
    }
  }
  goals('clean')
  goals('install')
  goals('-P developer')
  publishers {
    archiveJunit('**/target/(surefire|failsafe)-reports/*.xml') {
      retainLongStdout()
      testDataPublishers {
          publishTestStabilityData()
      }
    }
  }
}
