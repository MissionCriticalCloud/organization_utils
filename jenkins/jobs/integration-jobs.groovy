def FOLDER_NAME = 'mcc-github-organization'

def GITHUB_SLACK_INTEGRATION_JOB   = "${FOLDER_NAME}/github-slack-integration"
def GITHUB_JENKINS_INTEGRATION_JOB = "${FOLDER_NAME}/github-jenkins-integration"

def DEFAULT_GITHUB_ORGANIZATION_NAME = 'MissionCriticalCloud'
def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def DEFAULT_GITHUB_REPOSITORY        = 'MissionCriticalCloud/organization_utils'
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def DEFAULT_GROOVY_SCRIPT = 'scripts/manage-github-web-hooks.groovy'

def GITHUB_ORGANIZATION_NAME_PARAM = 'githubOrganizatioName'
def GITHUB_USER_NAME_PARAM         = 'githubUserName'
def WEBHOOK_SECRET_KEYWORD_PARAM   = 'webHookSecretKeyword'
def GITHUB_REPO_EVENTS             = 'repoEvents'

folder(FOLDER_NAME)

freeStyleJob(GITHUB_SLACK_INTEGRATION_JOB) {
  parameters {
    stringParam(GITHUB_ORGANIZATION_NAME_PARAM, DEFAULT_GITHUB_ORGANIZATION_NAME, 'The GitHub organization to manage')
    stringParam(GITHUB_USER_NAME_PARAM,         DEFAULT_GITHUB_USER_NAME,         'The GitHub user that manages the organization')
    stringParam(WEBHOOK_SECRET_KEYWORD_PARAM,   'SlackWebHook',                   'A keyword present in the plain text secret description, that identifies it to be a WebHook URL')
    textParam(GITHUB_REPO_EVENTS,               'COMMIT_COMMENT\nCREATE\nDELETE\nFORK\nISSUE_COMMENT\nISSUES\nMEMBER\nPULL_REQUEST\nPULL_REQUEST_REVIEW_COMMENT\nPUSH RELEASE\nREPOSITORY\nSTATUS\nTEAM_ADD\nWATCH', 'List of GtiHub events that should trigger WebHook call')
  }
  scm {
    git {
      remote {
        github(DEFAULT_GITHUB_REPOSITORY, 'https' )
      }
      branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
      shallowClone(true)
      clean(true)
    }
  }
  triggers {
    githubPush()
  }
  steps {
    systemGroovyScriptFile(DEFAULT_GROOVY_SCRIPT)
  }
}

freeStyleJob(GITHUB_JENKINS_INTEGRATION_JOB) {
  parameters {
    stringParam(GITHUB_ORGANIZATION_NAME_PARAM, DEFAULT_GITHUB_ORGANIZATION_NAME, 'The GitHub organization to manage')
    stringParam(GITHUB_USER_NAME_PARAM,         DEFAULT_GITHUB_USER_NAME,         'The GitHub user that manages the organization')
    stringParam(WEBHOOK_SECRET_KEYWORD_PARAM,   'JenkinsWebHook',                 'A keyword present in the plain text secret description, that identifies it to be a WebHook URL')
    textParam(GITHUB_REPO_EVENTS,               'ISSUE_COMMENT\nPULL_REQUEST',    'List of GtiHub events that should trigger WebHook call')
  }
  scm {
    git {
      remote {
        github(DEFAULT_GITHUB_REPOSITORY, 'https' )
      }
      branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
      shallowClone(true)
      clean(true)
    }
  }
  triggers {
    githubPush()
  }
  steps {
    systemGroovyScriptFile(DEFAULT_GROOVY_SCRIPT)
  }
}
