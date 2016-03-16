def FOLDER_NAME = 'mcc-bubble'

def TRACKING_REPO_UPDATE_JOB = "${FOLDER_NAME}/tracking-repo-update"
def SEED_JOB                 = "${FOLDER_NAME}/seed-job"

def ORGANIZATION_NAME                           = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME          = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY        = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH    = 'master'

def BUBBLE_BLUEPRINT_GITHUB_REPOSITORY          = "${ORGANIZATION_NAME}/bubble-blueprint"
def BUBBLE_BLUEPRINT_GITHUB_DEFAULT_BRANCH      = 'master'
def BUBBLE_COOKBOOK_GITHUB_REPOSITORY           = "${ORGANIZATION_NAME}/bubble-cookbook"
def BUBBLE_COOKBOOK_GITHUB_DEFAULT_BRANCH       = 'master'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'

def DEFAULT_EXECUTOR = 'executor'

folder(FOLDER_NAME)

freeStyleJob(SEED_JOB) {
  logRotator {
    numToKeep(10)
    artifactNumToKeep(10)
  }
  label(DEFAULT_EXECUTOR)
  scm {
    git {
      remote {
        github(ORGANIZATION_UTILS_GITHUB_REPOSITORY, 'https' )
      }
      branch(ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH)
      shallowClone(true)
      clean(true)
      configure { node ->
        node / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
          includedRegions 'jenkins/jobs/bubble_jobs[.]groovy'
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
      external('jenkins/jobs/bubble_jobs.groovy')
    }
  }
}

freeStyleJob(TRACKING_REPO_UPDATE_JOB) {
  displayName('bubble-blueprint tracking repo update')
  parameters {
    stringParam(BUBBLE_BLUEPRINT_GITHUB_REPOSITORY, BUBBLE_BLUEPRINT_GITHUB_DEFAULT_BRANCH, 'Branch to be built')
  }
  label(DEFAULT_EXECUTOR)
  concurrentBuild()
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
    cron('H/15 * * * *')
  }
  scm {
    git {
      remote {
        github(BUBBLE_BLUEPRINT_GITHUB_REPOSITORY, 'ssh' )
        credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
      }
      branch(BUBBLE_BLUEPRINT_GITHUB_DEFAULT_BRANCH)
      shallowClone(false)
      clean(true)
      recursiveSubmodules(true)
      trackingSubmodules(true)
    }
  }
  steps {
    shell(makeMultiline([
      'git config --global user.email "int-mccd_jenkins@schubergphilis.com"',
      'git config --global user.name "mccd-jenkins"',
      'if [ -z "$(git status -su)" ]; then',
      '  echo "==> No submodule changed"',
      'else',
      '  echo "==> Updating all submodules in remote repository"',
      'fi'
    ]))
  }
}

def makeMultiline(lines) {
  return listToStringWithSeparator('\n', lines)
}

def listToStringWithSeparator(separator, list) {
  return list.join(separator)
}
