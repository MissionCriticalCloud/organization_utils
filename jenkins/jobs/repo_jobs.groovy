def COSMIC_FOLDER_NAME = 'cosmic-repos'

def UPDATE_COSMIC_REPO_JOB = "${COSMIC_FOLDER_NAME}/update-cosmic-repo"
def SEED_JOB               = "${COSMIC_FOLDER_NAME}/seed-job"

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORG_UTILS_GITHUB_REPOSITORY      = 'MissionCriticalCloud/organization_utils'
def COSMIC_GITHUB_REPOSITORY         = 'MissionCriticalCloud/cosmic'
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

folder(COSMIC_FOLDER_NAME)

freeStyleJob(SEED_JOB) {
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
      external('jenkins/jobs/repo_jobs.groovy')
    }
  }
}

freeStyleJob(UPDATE_COSMIC_REPO_JOB) {
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
