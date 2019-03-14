def FOLDER_NAME = 'bubble-templates'

def WORKSPACE_VAR = '${WORKSPACE}'
def CUSTOM_WORKSPACE_PARAM = 'CUSTOM_WORKSPACE'

def PACKER_BUILD_JOB = "${FOLDER_NAME}/packer-build"
def PACKER_CRON_JOB = "${FOLDER_NAME}/packer-cron"
def SEED_JOB = "${FOLDER_NAME}/seed-job"

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'branch'

def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def BUBBLE_TEMPLATES_PACKER_NAME = 'bubble-templates-packer'
def BUBBLE_TEMPLATES_PACKER_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${BUBBLE_TEMPLATES_PACKER_NAME}"
def BUBBLE_TEMPLATES_PACKER_GITHUB_DEFAULT_BRANCH = 'master'

def TOP_LEVEL_COSMIC_JOBS_CATEGORY = 'top-level-cosmic-jobs'

def MCCD_JENKINS_GITHUB_CREDENTIALS = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'

def DEFAULT_EXECUTOR = 'executor'

def WORKSPACES = [
        '${WORKSPACE}/cosmic-centos-7',
        '${WORKSPACE}/cloudstack-centos-6'
]

def BUBBLE_TEMPLATES_BUILD_ARTEFACTS = [
        '*/packer_output/*'
]


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
                    includedRegions 'jenkins/jobs/bubble_templates_jobs[.]groovy'
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
            external('jenkins/jobs/bubble_templates_jobs.groovy')
        }
    }
}

multiJob(PACKER_CRON_JOB) {
    displayName('packer cron job')
    parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, BUBBLE_TEMPLATES_PACKER_GITHUB_DEFAULT_BRANCH, 'Branch to be built')
    }
    label(DEFAULT_EXECUTOR)
    concurrentBuild()
    throttleConcurrentBuilds {
        maxPerNode(1)
        categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
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
        cron('00 06 * * *')
    }
    scm {
        git {
            remote {
                github(BUBBLE_TEMPLATES_PACKER_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            }
            branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
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
    steps {
        phase('Build all templates') {
            WORKSPACES.each { workspace ->
                phaseJob(PACKER_BUILD_JOB) {
                    parameters {
                        predefinedProp(CUSTOM_WORKSPACE_PARAM, workspace)
                        sameNode()
                        gitRevision(true)
                    }
                }
            }
        }
    }
    publishers {
        archiveArtifacts {
            pattern(makePatternList(BUBBLE_TEMPLATES_BUILD_ARTEFACTS))
            onlyIfSuccessful()
        }
    }
}

freeStyleJob(PACKER_BUILD_JOB) {
    displayName('packer build job')
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    steps {
        shell('bash -x build.sh')
        shell('cd packer_output; md5sum * > `ls`.md5')
    }
}

def injectJobVariable(variableName) {
    return '${' + variableName + '}'
}

def makePatternList(patterns) {
    return listToStringWithSeparator(', ', patterns)
}

def listToStringWithSeparator(separator, list) {
    return list.join(separator)
}
