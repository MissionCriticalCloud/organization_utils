def CUSTOM_WORKSPACE_PARAM = 'CUSTOM_WORKSPACE'

def WORKSPACE_VAR = '${WORKSPACE}'

def FOLDER_NAME = 'cosmic-helm'

def TRACKING_REPO_UPDATE_JOB = "${FOLDER_NAME}/cosmic-helm-repo-update"
def SEED_JOB = "${FOLDER_NAME}/seed-job"

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'branch'

def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def COSMIC_HELM_CHARTS_NAME = 'cosmic-microservices-chart'
def COSMIC_HELM_CHARTS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${COSMIC_HELM_CHARTS_NAME}"
def COSMIC_HELM_CHARTS_GITHUB_DEFAULT_BRANCH = 'master'
def COSMIC_HELM_AWS_REPO = 'cosmic-helm-repository.cosmiccloud.io'

def GIT_PR_BRANCH_ENV_VARIABLE_NAME = 'ghprbActualCommit'
def GIT_REPO_BRANCH_PARAM = 'branch'
def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'


def TOP_LEVEL_COSMIC_JOBS_CATEGORY = 'top-level-cosmic-jobs'

def MCCD_JENKINS_GITHUB_CREDENTIALS = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_AWS_CREDENTIALS = '948b39fd-61fd-4669-8afd-55e46c1c03ee'

def DEFAULT_EXECUTOR = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

def cosmicMasterBuild = "0001-cosmic-helm-repo-master-build"
def cosmicPullRequestBuild = "0002-cosmic-helm-repo-pull-request-build"

def fullBuild = "${FOLDER_NAME}/0020-full-build"
def makeBuild = "${FOLDER_NAME}/9997-make-build"
def makeLintBuild = "${FOLDER_NAME}/9997-make-lint-build"

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
                github(ORGANIZATION_UTILS_GITHUB_REPOSITORY, 'https')
            }
            branch(ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH)
            configure { node ->
                node / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
                    includedRegions 'jenkins/jobs/bubble_jobs[.]groovy'
                    excludedRegions ''
                }
            }
            extensions {
                cleanAfterCheckout()
                cleanBeforeCheckout()
                cloneOptions {
                    shallow(true)
                }
                wipeOutWorkspace()
            }
        }
    }
    triggers {
        githubPush()
    }
    steps {
        dsl {
            external('jenkins/jobs/cosmic_helm_repo_jobs.groovy')
        }
    }
}

multiJob("${FOLDER_NAME}/" + cosmicMasterBuild) {
    label(DEFAULT_EXECUTOR_MCT)
    concurrentBuild()
    logRotator {
        numToKeep(50)
        artifactNumToKeep(20)
    }
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    triggers {
      githubPush()
    }
    scm {
        git {
            remote {
                github(COSMIC_HELM_CHARTS_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                name('origin')
                refspec('+refs/heads/master')
            }
            branch('master')
            extensions {
                wipeOutWorkspace()
            }
        }
    }
    steps {
        phase('Full Build') {
            phaseJob(fullBuild) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                    gitRevision(true)
                }
            }
        }
    }
}

multiJob("${FOLDER_NAME}/" + cosmicPullRequestBuild) {
    parameters {
        stringParam(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_PR_BRANCH_ENV_VARIABLE_NAME), 'Branch to be built')
    }
    concurrentBuild()
    throttleConcurrentBuilds {
        categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
    }
    label(DEFAULT_EXECUTOR_MCT)
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    triggers {
        githubPullRequest {
            triggerPhrase('go build')
            useGitHubHooks()
            permitAll()
            extensions {
                commitStatus {
                    context(DEFAULT_GITHUB_JOB_LABEL)
                    startedStatus('building...')
                    completedStatus('SUCCESS', 'All is well')
                    completedStatus('FAILURE', 'Something went wrong. Investigate!')
                }
            }
        }
    }
    scm {
        git {
            remote {
                github(COSMIC_HELM_CHARTS_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                name('origin')
                refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
            }
            branch(injectJobVariable(GIT_REPO_BRANCH_PARAM))
            extensions {
                wipeOutWorkspace()
            }
        }
    }
    steps {
        phase('PR Build') {
            phaseJob(makeLintBuild) {
                parameters {
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                    sameNode()
                    gitRevision(true)
                }
            }
        }
    }
}

multiJob(fullBuild) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
        stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(DEFAULT_EXECUTOR_MCT)
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
    steps {
        phase('Build Cosmic Helm repo files') {
            phaseJob(makeBuild) {
                currentJobParameters(false)
                parameters {
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                    sameNode()
                    gitRevision(true)
                }
            }
        }
//        Next phase will be pushing artifacts to S3
    }
}

freeStyleJob(makeBuild) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(DEFAULT_EXECUTOR_MCT)
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
    steps {
        shell('helm init -c')
        shell('make')
    }
}

freeStyleJob(makeLintBuild) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(DEFAULT_EXECUTOR_MCT)
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
    steps {
        shell('helm init -c')
        shell('make lint')
    }
}



def makeMultiline(lines) {
    return listToStringWithSeparator('\n', lines)
}

def listToStringWithSeparator(separator, list) {
    return list.join(separator)
}

def injectJobVariable(variableName) {
    return '${' + variableName + '}'
}
