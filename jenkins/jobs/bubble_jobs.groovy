def FOLDER_NAME = 'mcc-bubble'

def TRACKING_REPO_UPDATE_JOB = "${FOLDER_NAME}/tracking-repo-update"
def SEED_JOB = "${FOLDER_NAME}/seed-job"

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'branch'

def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def BUBBLE_BLUEPRINT_NAME = 'bubble-blueprint'
def BUBBLE_BLUEPRINT_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${BUBBLE_BLUEPRINT_NAME}"
def BUBBLE_BLUEPRINT_GITHUB_DEFAULT_BRANCH = 'master'

def BUBBLE_COOKBOOK_NAME = 'bubble'

def MCCD_JENKINS_GITHUB_CREDENTIALS = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'

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
            external('jenkins/jobs/bubble_jobs.groovy')
        }
    }
}

freeStyleJob(TRACKING_REPO_UPDATE_JOB) {
    displayName('bubble-blueprint tracking repo update')
    parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, BUBBLE_BLUEPRINT_GITHUB_DEFAULT_BRANCH, 'Branch to be built')
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
                github(BUBBLE_BLUEPRINT_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            }
            branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
            extensions {
                cleanAfterCheckout()
                cleanBeforeCheckout()
                cloneOptions {
                    shallow(true)
                }
                submoduleOptions {
                    recursive(true)
                    tracking(true)
                }
                wipeOutWorkspace()
            }
        }
    }
    steps {
        shell(makeMultiline([
                "git config --global user.email \"int-mccd_jenkins@schubergphilis.com\"",
                "git config --global user.name \"mccd-jenkins\"",
                "export LANG=en_US.UTF-8",
                "if [ -z \"\$(git status -su)\" ]; then",
                "  echo \"==> No submodule changed\"",
                "else",
                "  echo \"==> Submodule has changed in remote repository\"",
                "",
                "  echo \"=> Clean outdated cookbooks\"",
                "  find chef-repo/cookbooks/* -maxdepth 0 -type d | grep -v '${BUBBLE_COOKBOOK_NAME}' | xargs rm -rf",
                "",
                "  echo \"=> Clean berkshelf cache\"",
                "  rm -rf ~/.berkshelf/cookbooks/*",
                "",
                "  TMP_UUID=\"/tmp/\$(uuidgen)\"",
                "  echo \"=> Create tmp working directory \$TMP_UUID\"",
                "  mkdir \$TMP_UUID",
                "",
                "  echo \"=> Berks vendor the cookbook\"",
                "  berks vendor --berksfile=chef-repo/cookbooks/${BUBBLE_COOKBOOK_NAME}/Berksfile \$TMP_UUID",
                "",
                "  echo \"=> Move vendored cookbooks to tracking repo\"",
                "  find \$TMP_UUID/* -maxdepth 0 -type d | grep -v '${BUBBLE_COOKBOOK_NAME}' | xargs -I '{}' mv '{}' chef-repo/cookbooks/",
                "",
                "  echo \"=> Commit and push update\"",
                "  git add .",
                "  git commit -m \"Update all submodules to latest HEAD\"",
                "  git push origin HEAD:${injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM)}",
                "fi"
        ]))
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
