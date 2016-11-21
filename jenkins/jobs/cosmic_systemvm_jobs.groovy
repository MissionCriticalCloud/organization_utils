def FOLDER_NAME = 'cosmic-systemvm'

def PACKER_BUILD_JOB = "${FOLDER_NAME}/packer-build"
def SEED_JOB = "${FOLDER_NAME}/seed-job"

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'branch'

def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def SYSTEMVM_PACKER_NAME = 'systemvm-packer'
def SYSTEMVM_PACKER_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${SYSTEMVM_PACKER_NAME}"
def SYSTEMVM_PACKER_GITHUB_DEFAULT_BRANCH = 'master'

def MCCD_JENKINS_GITHUB_CREDENTIALS = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'

def DEFAULT_EXECUTOR = 'executor-mct'

def SYSTEMVM_BUILD_ARTEFACTS = [
        'packer_output/*'
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
                    includedRegions 'jenkins/jobs/cosmic_systemvm_jobs[.]groovy'
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
            external('jenkins/jobs/cosmic_systemvm_jobs.groovy')
        }
    }
}

freeStyleJob(PACKER_BUILD_JOB) {
    displayName('packer build job')
    parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, SYSTEMVM_PACKER_GITHUB_DEFAULT_BRANCH, 'Branch to be built')
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
        cron('00 06 * * *')
    }
    scm {
        git {
            remote {
                github(SYSTEMVM_PACKER_GITHUB_REPOSITORY, 'ssh')
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
        shell('bash -x build.sh')
        shell('cd packer_output; md5sum * > md5.txt')
    }
    publishers {
        archiveArtifacts {
            pattern(makePatternList(SYSTEMVM_BUILD_ARTEFACTS))
            onlyIfSuccessful()
        }
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
