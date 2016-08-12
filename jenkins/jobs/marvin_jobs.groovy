def FOLDER_NAME = 'cosmic-marvin'

def MASTER_TRIGGER_MARVIN_JOB   = "${FOLDER_NAME}/0000-master-build-trigger-marvin"
def MASTER_TRIGGER_COSMIC_JOB   = "${FOLDER_NAME}/0001-master-build-trigger-cosmic"
def RELEASE_JOB                 = "${FOLDER_NAME}/0008-release-build"
def MARVIN_BUILD_JOB            = "${FOLDER_NAME}/0009-marvin-build-job"
def CLEAN_WORKSPACE_JOB         = "${FOLDER_NAME}/0010-clean-workspace-job"
def PULL_MARVIN_JOB             = "${FOLDER_NAME}/0020-pull-marvin-job"
def PULL_COSMIC_JOB             = "${FOLDER_NAME}/0021-pull-cosmic-job"
def BUILD_COSMIC_JOB            = "${FOLDER_NAME}/0022-build-cosmic-job"
def BUMP_RELEASE_VERSION_JOB    = "${FOLDER_NAME}/0030-bump-release-version-job"
def COMMIT_RELEASE_VERSION_JOB  = "${FOLDER_NAME}/0031-commit-release-version-job"
def BUMP_SNAPSHOT_VERSION_JOB   = "${FOLDER_NAME}/0032-bump-snapshot-version-job"
def COMMIT_SNAPSHOT_VERSION_JOB = "${FOLDER_NAME}/0033-commit-snapshot-version-job"
def BUILD_API_JOB               = "${FOLDER_NAME}/0040-build-api-job"
def COMMIT_API_CHANGES_JOB      = "${FOLDER_NAME}/0041-commit-api-changes-job"
def PYTHON_TESTS_JOB            = "${FOLDER_NAME}/0050-python-tests-job"
def PYTHON_EGG_JOB              = "${FOLDER_NAME}/0051-python-egg-job"
def PUSH_RELEASE_NEXUS_JOB      = "${FOLDER_NAME}/0060-push-release-nexus-job"
def PUSH_SNAPSHOT_NEXUS_JOB     = "${FOLDER_NAME}/0061-push-snapshot-nexus-job"
def SEED_JOB                    = "${FOLDER_NAME}/9991-seed-job"

def GIT_REPO_BRANCH_PARAM = 'sha1'

def CUSTOM_WORKSPACE_PARAM = 'CUSTOM_WORKSPACE'
def WORKSPACE_VAR          = '${WORKSPACE}'

def ORGANIZATION_NAME                        = 'MissionCriticalCloud'
def MARVIN_REPOSITORY_NAME                   = 'marvin'
def MARVIN_GITHUB_REPOSITORY                 = "${ORGANIZATION_NAME}/${MARVIN_REPOSITORY_NAME}"
def COSMIC_REPOSITORY_NAME                   = 'cosmic'
def COSMIC_GITHUB_REPOSITORY                 = "${ORGANIZATION_NAME}/${COSMIC_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_REPOSITORY_NAME       = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY     = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def BETA_NEXUS_SNAPSHOT_URL = 'https://beta-nexus.mcc.schubergphilis.com/content/repositories/snapshots/'
def BETA_NEXUS_RELEASE_URL  = 'https://beta-nexus.mcc.schubergphilis.com/content/repositories/releases/'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'

def DEFAULT_EXECUTOR = 'executor-mct'

def RELEASE_VERSION_PARAM  = 'releaseVersion'

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
                    includedRegions 'jenkins/jobs/marvin_jobs[.]groovy'
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
            external('jenkins/jobs/marvin_jobs.groovy')
        }
    }
}

multiJob(MASTER_TRIGGER_MARVIN_JOB) {
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
        githubPush()
    }
    scm {
        git {
            remote {
                github(MARVIN_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                name('origin')
                refspec('+refs/heads/master')
            }
            extensions {
                cleanAfterCheckout()
                cleanBeforeCheckout()
                wipeOutWorkspace()
            }
            branch('master')
        }
    }
    steps {
        downstreamParameterized {
            trigger(MARVIN_BUILD_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                }
                block {
                    buildStepFailure('FAILURE')
                }
            }
        }
    }
}

multiJob(MASTER_TRIGGER_COSMIC_JOB) {
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
        githubPush()
    }
    scm {
        git {
            remote {
                github(COSMIC_GITHUB_REPOSITORY, 'ssh')
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                name('origin')
                refspec('+refs/heads/master')
            }
            extensions {
                cleanAfterCheckout()
                cleanBeforeCheckout()
                wipeOutWorkspace()
            }
            recursiveSubmodules(true)
            trackingSubmodules(false)
            branch('master')
        }
    }
    steps {
        downstreamParameterized {
            trigger(MARVIN_BUILD_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                }
                block {
                    buildStepFailure('FAILURE')
                }
            }
        }
    }
}

multiJob(MARVIN_BUILD_JOB) {
    parameters {
        stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
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
    steps {
        phase('Clean workspace') {
            phaseJob(CLEAN_WORKSPACE_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                }
            }
        }
        phase('Pull repositories') {
            phaseJob(PULL_MARVIN_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                    predefinedProp(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_REPO_BRANCH_PARAM))
                }
            }
            phaseJob(PULL_COSMIC_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic")
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                }
            }
        }
        phase('Build Cosmic') {
            phaseJob(BUILD_COSMIC_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic")
                }
            }

        }
        phase('Build API files') {
            phaseJob(BUILD_API_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin/marvin")
                }
            }
        }
        phase('Commit API files') {
            phaseJob(COMMIT_API_CHANGES_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Test Python code') {
            phaseJob(PYTHON_TESTS_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Build Python EGG') {
            phaseJob(PYTHON_EGG_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Push artifact to Nexus') {
            phaseJob(PUSH_SNAPSHOT_NEXUS_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
    }
    publishers {
        archiveArtifacts {
            pattern('marvin/dist/*')
        }
        archiveXUnit {
            jUnit {
                pattern('marvin/nosetests.xml')
            }
        }
    }
}

multiJob(RELEASE_JOB) {
    parameters {
        stringParam(RELEASE_VERSION_PARAM, '', 'Version number to use for release')
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
    steps {
        phase('Clean workspace') {
            phaseJob(CLEAN_WORKSPACE_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                }
            }
        }
        phase('Pull repositories') {
            phaseJob(PULL_MARVIN_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                }
            }
            phaseJob(PULL_COSMIC_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic")
                    predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
                }
            }
        }
        phase('Build Cosmic') {
            phaseJob(BUILD_COSMIC_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic")
                }
            }

        }
        phase('Set release version') {
            phaseJob(BUMP_RELEASE_VERSION_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                    predefinedProp(RELEASE_VERSION_PARAM, injectJobVariable(RELEASE_VERSION_PARAM))
                }
            }
        }
        phase('Commit release version') {
            phaseJob(COMMIT_RELEASE_VERSION_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                    predefinedProp(RELEASE_VERSION_PARAM, injectJobVariable(RELEASE_VERSION_PARAM))
                }
            }
        }
        phase('Build API files') {
            phaseJob(BUILD_API_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin/marvin")
                }
            }
        }
        phase('Commit API files') {
            phaseJob(COMMIT_API_CHANGES_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Test Python code') {
            phaseJob(PYTHON_TESTS_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Build Python EGG') {
            phaseJob(PYTHON_EGG_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Push artifact to Nexus') {
            phaseJob(PUSH_RELEASE_NEXUS_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Set snapshot version') {
            phaseJob(BUMP_SNAPSHOT_VERSION_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
        phase('Commit snapshot version') {
            phaseJob(COMMIT_SNAPSHOT_VERSION_JOB) {
                parameters {
                    sameNode()
                    predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/marvin")
                }
            }
        }
    }
    publishers {
        archiveArtifacts {
            pattern('marvin/dist/*')
        }
        archiveXUnit {
            jUnit {
                pattern('marvin/nosetests.xml')
            }
        }
    }
}

freeStyleJob(CLEAN_WORKSPACE_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                "rm -rf ./*",
                "mkdir ./marvin",
                "mkdir ./cosmic"
        ]))
    }
}

freeStyleJob(PULL_MARVIN_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
        stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    scm {
        git {
            remote {
                github(MARVIN_GITHUB_REPOSITORY, 'ssh' )
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            }
            branch(injectJobVariable(GIT_REPO_BRANCH_PARAM))
        }
    }
    steps {
    }
}

freeStyleJob(PULL_COSMIC_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
        stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    scm {
        git {
            remote {
                github(COSMIC_GITHUB_REPOSITORY, 'ssh' )
                credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            }
            branch(injectJobVariable(GIT_REPO_BRANCH_PARAM))
            shallowClone(false)
            recursiveSubmodules(true)
            trackingSubmodules(true)
        }
    }
    steps {
    }
}

mavenJob(BUILD_COSMIC_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    archivingDisabled(true)
    concurrentBuild(true)
    goals('clean')
    goals('package')
    goals('-T4')
    goals('-Psystemvm')
}

freeStyleJob(BUMP_RELEASE_VERSION_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
        stringParam(RELEASE_VERSION_PARAM, '', 'Version number to use for release')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                "VERSION=" + injectJobVariable(RELEASE_VERSION_PARAM),
                "if [ -z \"\${VERSION}\" ]; then",
                "    echo \"No release version specified, using snapshot version for release.\"",
                "    ",
                "    VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\" | sed \'s/-SNAPSHOT//g\')",
                "    sed -i \"s/VERSION = .*/VERSION = \\\'\${VERSION}\\\'/g\" setup.py",
                "else",
                "    echo \"Release version \${VERSION} specified.\"",
                "    ",
                "    sed -i \"s/VERSION = .*/VERSION = \\\'\${VERSION}\\\'/g\" setup.py",
                "fi"
        ]))
    }
}

freeStyleJob(COMMIT_RELEASE_VERSION_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
        stringParam(RELEASE_VERSION_PARAM, '', 'Version number to use for release')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                "git config --global user.email \"int-mccd_jenkins@schubergphilis.com\"",
                "git config --global user.name \"mccd-jenkins\"",
                "if [ -z \"\$(git status -su)\" ]; then",
                "    echo \"==> [ERROR] Version didn\'t change during release.\"",
                "else",
                "    VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\" | sed \'s/-SNAPSHOT//g\')",
                "    ",
                "    echo \"==> Committing new Release version \${VERSION} to repository\"",
                "    git add setup.py",
                "    git commit -m \"New release version \${VERSION}\"",
                "    git push origin HEAD:master",
                "fi"
        ]))
    }
}

freeStyleJob(BUILD_API_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell("rm -rf ./cloudstackAPI")
        shell("python codegenerator.py -s ../../cosmic/cosmic-core/apidoc/target/commands.xml")
    }
}

freeStyleJob(COMMIT_API_CHANGES_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                'git config --global user.email "int-mccd_jenkins@schubergphilis.com"',
                'git config --global user.name "mccd-jenkins"',
                'if [ -z "$(git status -su)" ]; then',
                '  echo "==> No API changes"',
                'else',
                '  echo "==> Committing API changes in remote repository"',
                '  git add marvin/cloudstackAPI',
                '  git commit -m "Update all API files"',
                '  git push origin HEAD:master',
                'fi'
        ]))
    }
}

freeStyleJob(PYTHON_TESTS_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell('nosetests -v --with-xunit tests')
    }
    publishers {
        archiveXUnit {
            jUnit {
                pattern('nosetests.xml')
            }
        }
    }
}

freeStyleJob(PYTHON_EGG_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell("python setup.py sdist")
    }
}

freeStyleJob(PUSH_SNAPSHOT_NEXUS_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    concurrentBuild(true)
    steps {
        shell(makeMultiline([
                "SNAPSHOT_VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\")",
                "",
                "mvn deploy:deploy-file \\",
                "    -Durl=" + BETA_NEXUS_SNAPSHOT_URL + " \\",
                "    -DrepositoryId=beta-nexus \\",
                "    -DgroupId=cloud.cosmic \\",
                "    -DartifactId=cloud-marvin \\",
                "    -Dversion=\${SNAPSHOT_VERSION} \\",
                "    -Dpackaging=tar.gz \\",
                "    -Dfile=dist/Marvin-\${SNAPSHOT_VERSION}.tar.gz"
        ]))
    }
}

freeStyleJob(PUSH_RELEASE_NEXUS_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
        colorizeOutput('xterm')
        timestamps()
    }
    concurrentBuild(true)
    steps {
        shell(makeMultiline([
                "RELEASE_VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\")",
                "",
                "mvn deploy:deploy-file \\",
                "    -Durl=" + BETA_NEXUS_RELEASE_URL + " \\",
                "    -DrepositoryId=beta-nexus \\",
                "    -DgroupId=cloud.cosmic \\",
                "    -DartifactId=cloud-marvin \\",
                "    -Dversion=\${RELEASE_VERSION} \\",
                "    -Dpackaging=tar.gz \\",
                "    -Dfile=dist/Marvin-\${RELEASE_VERSION}.tar.gz"
        ]))
    }
}

freeStyleJob(BUMP_SNAPSHOT_VERSION_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                "VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\")",
                "MINOR_VERSION=\$(echo \$VERSION | awk -F \\. {'print \$4'})",
                "MINOR_VERSION=\$(expr \$MINOR_VERSION + 1)-SNAPSHOT",
                "VERSION=\$(echo \$VERSION | sed \"s/\\.[^.]*\$//\")",
                "VERSION=\$VERSION.\$MINOR_VERSION",
                "sed -i \"s/VERSION = .*/VERSION = \\\'\${VERSION}\\\'/g\" setup.py",
        ]))
    }
}

freeStyleJob(COMMIT_SNAPSHOT_VERSION_JOB) {
    parameters {
        stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
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
    steps {
        shell(makeMultiline([
                "git config --global user.email \"int-mccd_jenkins@schubergphilis.com\"",
                "git config --global user.name \"mccd-jenkins\"",
                "if [ -z \"\$(git status -su)\" ]; then",
                "    echo \"==> [ERROR] Version didn\'t change during release.\"",
                "else",
                "    VERSION=\$(grep \"VERSION = \" setup.py | grep -o \"\'.*\'\" | sed \"s/\'//g\" | sed \'s/-SNAPSHOT//g\')",
                "    echo \"==> Committing new snapshot version to repository\"",
                "    git add setup.py",
                "    git commit -m \"New snapshot version \${VERSION}\"",
                "    git push origin HEAD:master",
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
