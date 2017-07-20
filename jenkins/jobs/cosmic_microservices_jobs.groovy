def GIT_REPO_BRANCH_PARAM = 'branch'
def CUSTOM_WORKSPACE_PARAM = 'CUSTOM_WORKSPACE'

def WORKSPACE_VAR = '${WORKSPACE}'

def ORGANIZATION_REPO_NAME = 'organization_utils'
def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORG_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_REPO_NAME}"
def COSMIC_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/cosmic-microservices"
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_OAUTH2_CREDENTIAL_PARAM = 'mccdJenkinsOauth2'
def SONAR_RUNNER_PASSWORD_PARAM = 'sonarRunnerPassword'

def TOP_LEVEL_COSMIC_JOBS_CATEGORY = 'top-level-cosmic-jobs'

def MAVEN_RELEASE_VERSION_PARAM = 'releaseVersion'
def MAVEN_RELEASE_NO_PUSH = '-DpushChanges=false -DlocalCheckout=true'

def MAVEN_SNAPSHOT_VERSION_PARAM = 'snapshotVersion'

def GIT_BRANCH_ENV_VARIABLE_NAME = 'GIT_BRANCH'
def GIT_PR_BRANCH_ENV_VARIABLE_NAME = 'ghprbActualCommit'

def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'

def MCCD_JENKINS_GITHUB_CREDENTIALS = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'
def SONAR_RUNNER_PASSWORD_CREDENTIALS = 'df77a17c-5613-4fdf-8c49-52789b613e51'

def MAVEN_REPORTS = [
        '**/target/surefire-reports/*.xml',
        '**/target/failsafe-reports/*.xml'
]

def MARVIN_REPORTS = [
//        'nosetests-required_hardware*'
]

def XUNIT_REPORTS = MARVIN_REPORTS + MAVEN_REPORTS

def COSMIC_BUILD_ARTEFACTS = [
        'cosmic-config-server/target/cosmic-config-server.jar',
        'cosmic-metrics-collector/target/cosmic-metrics-collector.jar',
        'cosmic-usage-api/target/cosmic-usage-api.jar',
        'cosmic-bill-viewer/target/cosmic-bill-viewer.jar'
]


def DEFAULT_EXECUTOR = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

def DOCKER_HOST = 'tcp://127.0.0.1:2375'
def DOCKER_PUSH = 'mvn docker:push -Ddocker.filter=%g/%a:%l -Pproduction'
def DOCKER_PUSH_PARAM = 'dockerPushParam'

// dev folder is to play with jobs.
// Jobs defined there should never automatically trigger
def FOLDERS = [
        'cosmic-microservices',
        'cosmic-microservices-dev'
]

FOLDERS.each { folderName ->

    def cosmicView = "Cosmic-Microservices"

    def cosmicmicroservicesMasterBuild = "0001-cosmic-microservices-master-build"
    def cosmicmicroservicesPullRequestBuild = "0002-cosmic-microservices-pull-request-build"
    def cosmicmicroservicesReleaseBuild = "0003-cosmic-microservices-release-build"
    def cosmicmicroservicesSnapshotBuild = "0004-cosmic-microservices-snapshot-build"

    def fullBuild = "${folderName}/0020-full-build"
    def mavenBuild = "${folderName}/9997-maven-build"
    def mavenSonarBuild = "${folderName}/9998-maven-sonar-build"
    def seedJob = "${folderName}/9999-seed-job"

    def isDevFolder = folderName.endsWith('-dev')
    def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')

    folder(folderName) {
        primaryView(cosmicView)
    }

    listView("${folderName}/${cosmicView}") {
        description('Cosmic build and release jobs.')
        jobs {
            name(cosmicmicroservicesMasterBuild)
            name(cosmicmicroservicesPullRequestBuild)
            name(cosmicmicroservicesReleaseBuild)
            name(cosmicmicroservicesSnapshotBuild)
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }

        // seed job is meant to trigger when this file changes in git
        freeStyleJob(seedJob) {
            logRotator {
                numToKeep(50)
                artifactNumToKeep(10)
            }
            label(DEFAULT_EXECUTOR)
            if (!isDevFolder) {
                scm {
                    git {
                        remote {
                            github(ORG_UTILS_GITHUB_REPOSITORY, 'https')
                        }
                        branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
                        extensions {
                            cleanAfterCheckout()
                            cleanBeforeCheckout()
                            cloneOptions {
                                shallow(true)
                            }
                            wipeOutWorkspace()
                        }
                        configure { node ->
                            node / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
                                includedRegions 'jenkins/jobs/cosmic_microservices_jobs[.]groovy'
                                excludedRegions ''
                            }
                        }
                    }
                }
                triggers {
                    githubPush()
                }
            }
            steps {
                dsl {
                    if (!isDevFolder) {
                        external('jenkins/jobs/cosmic_microservices_jobs.groovy')
                    }
                }
            }
        }

        multiJob("${folderName}/" + cosmicmicroservicesMasterBuild) {
            label(executorLabelMct)
            concurrentBuild()
            throttleConcurrentBuilds {
                categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
            }
            logRotator {
                numToKeep(50)
                artifactNumToKeep(20)
            }
            wrappers {
                colorizeOutput('xterm')
                timestamps()
            }
            triggers {
                if (!isDevFolder) {
                    githubPush()
                }
            }
            scm {
                git {
                    remote {
                        github(COSMIC_GITHUB_REPOSITORY, 'ssh')
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
                            predefinedProp(DOCKER_PUSH_PARAM, DOCKER_PUSH)
                            gitRevision(true)
                        }
                    }
                }
            }
            if (!isDevFolder) {
                publishers {
                    archiveArtifacts {
                        pattern(makePatternList(COSMIC_BUILD_ARTEFACTS))
                        onlyIfSuccessful()
                    }
                    archiveJunit(makePatternList(XUNIT_REPORTS)) {
                        retainLongStdout()
                        testDataPublishers {
                            publishTestStabilityData()
                        }
                    }
                    slackNotifications {
                        notifyBuildStart()
                        notifyAborted()
                        notifyFailure()
                        notifyNotBuilt()
                        notifyUnstable()
                        notifyBackToNormal()
                        includeTestSummary()
                        showCommitList()
                    }
                    githubCommitNotifier()
                }
            }
        }

        multiJob("${folderName}/" + cosmicmicroservicesPullRequestBuild) {
            parameters {
                stringParam(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_PR_BRANCH_ENV_VARIABLE_NAME), 'Branch to be built')
            }
            concurrentBuild()
            throttleConcurrentBuilds {
                categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
            }
            label(executorLabelMct)
            logRotator {
                numToKeep(50)
                artifactNumToKeep(10)
            }
            wrappers {
                colorizeOutput('xterm')
                timestamps()
            }
            triggers {
                if (!isDevFolder) {
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
            }
            scm {
                git {
                    remote {
                        github(COSMIC_GITHUB_REPOSITORY, 'ssh')
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
                phase('Full Build') {
                    phaseJob(fullBuild) {
                        parameters {
                            sameNode()
                            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                            predefinedProp(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_BRANCH_ENV_VARIABLE_NAME))
                            gitRevision(true)
                        }
                    }
                }
            }
            if (!isDevFolder) {
                publishers {
                    archiveJunit(makePatternList(XUNIT_REPORTS)) {
                        retainLongStdout()
                        testDataPublishers {
                            publishTestStabilityData()
                        }
                    }
                    slackNotifications {
                        notifyBuildStart()
                        notifyAborted()
                        notifyFailure()
                        notifyNotBuilt()
                        notifyUnstable()
                        notifyBackToNormal()
                        includeTestSummary()
                        showCommitList()
                    }
                }
            }
        }

        mavenJob("${folderName}/" + cosmicmicroservicesReleaseBuild) {
            parameters {
                stringParam(MAVEN_RELEASE_VERSION_PARAM, "", 'Release version')
            }
            concurrentBuild(false)
            throttleConcurrentBuilds {
                categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
            }
            label(executorLabelMct)
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
                        github(COSMIC_GITHUB_REPOSITORY, 'ssh')
                        credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                        name('origin')
                        refspec('+refs/heads/*:refs/remotes/origin/*')
                    }
                    branch("master")
                    extensions {
                        wipeOutWorkspace()
                    }
                }
            }
            environmentVariables {
                env('DOCKER_HOST', DOCKER_HOST)
            }
            preBuildSteps {
                shell("git checkout master")
            }
            goals("-Pproduction")
            goals("release:prepare release:perform -DreleaseVersion=${injectJobVariable(MAVEN_RELEASE_VERSION_PARAM)} ${(isDevFolder ? MAVEN_RELEASE_NO_PUSH : '')}")
            postBuildSteps {
                shell("git reset HEAD~1 --hard")
                shell("${DOCKER_PUSH}")
            }
        }

        mavenJob("${folderName}/" + cosmicmicroservicesSnapshotBuild) {
            parameters {
                stringParam(MAVEN_SNAPSHOT_VERSION_PARAM, "", 'Snapshot version, include \'-SNAPSHOT\' in the version string. Example: 6.0.0.0-SNAPSHOT')
            }
            concurrentBuild(false)
            throttleConcurrentBuilds {
                categories([TOP_LEVEL_COSMIC_JOBS_CATEGORY])
            }
            label(executorLabelMct)
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
                        github(COSMIC_GITHUB_REPOSITORY, 'ssh')
                        credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
                        name('origin')
                        refspec('+refs/heads/*:refs/remotes/origin/*')
                    }
                    branch("master")
                    extensions {
                        wipeOutWorkspace()
                    }
                }
            }
            environmentVariables {
                env('DOCKER_HOST', DOCKER_HOST)
            }
            preBuildSteps {
                shell("git checkout master")
            }
            goals("-Pproduction")
            goals("release:update-versions --batch-mode -DdevelopmentVersion=${injectJobVariable(MAVEN_SNAPSHOT_VERSION_PARAM)}")
            postBuildSteps {
                shell("git add .")
                shell("git commit -m \"Update SNAPSHOT version to ${injectJobVariable(MAVEN_SNAPSHOT_VERSION_PARAM)}\"")
                shell("git push origin HEAD:master")
            }
        }

        // Build for a branch of cosmic-microservices
        multiJob(fullBuild) {
            parameters {
                stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
                stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
                stringParam(DOCKER_PUSH_PARAM, '', 'If we want to push to Docker')
            }
            customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
            label(executorLabelMct)
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
                phase('Build maven project and prepare infrastructure for integrations tests') {
                    phaseJob(mavenBuild) {
                        currentJobParameters(true)
                        parameters {
                            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                            predefinedProp(DOCKER_PUSH_PARAM, injectJobVariable(DOCKER_PUSH_PARAM))
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
                phase('Sonar analysis') {
                    phaseJob(mavenSonarBuild) {
                        currentJobParameters(true)
                        parameters {
                            predefinedProp(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_REPO_BRANCH_PARAM))
                            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
            }
        }
    }

    // generic Maven job that builds on a folder (instead of a git repo)
    // this job is meant to be called by another job that already checked out a maven project
    mavenJob(mavenBuild) {
        parameters {
            credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
                type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
                required()
                defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
                description('mccd jenkins OAuth2 token credential')
            }
            stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
            stringParam(DOCKER_PUSH_PARAM, "", 'If we want to push to Docker')
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
        environmentVariables {
            env('DOCKER_HOST', DOCKER_HOST)
        }
        customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
        archivingDisabled(true)
        concurrentBuild(true)
        goals('clean')
        goals('install')
        goals('-U')
        goals('-Psonar-ci-cosmic-microservices,production')
        goals("-Dcosmic-microservices.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
        goals("-Dcosmic.metrics-collector.enable-collectors=false")
        postBuildSteps {
            shell("${injectJobVariable(DOCKER_PUSH_PARAM)}")
        }
    }

    mavenJob(mavenSonarBuild) {
        parameters {
            credentialsParam(SONAR_RUNNER_PASSWORD_PARAM) {
                type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
                required()
                defaultValue(SONAR_RUNNER_PASSWORD_CREDENTIALS)
                description('sonar-runner user credentials')
            }
            stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
            stringParam(GIT_REPO_BRANCH_PARAM, 'sha1', 'Branch to be built')
        }
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
        concurrentBuild()
        wrappers {
            colorizeOutput('xterm')
            timestamps()
            credentialsBinding {
                usernamePassword('SONAR_RUNNER_USERNAME', 'SONAR_RUNNER_PASSWORD', injectJobVariable(SONAR_RUNNER_PASSWORD_PARAM))
            }
        }
        environmentVariables {
            env('DOCKER_HOST', DOCKER_HOST)
        }
        customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
        archivingDisabled(true)
        goals('org.jacoco:jacoco-maven-plugin:merge@merge-integration-test-coverage')
        goals('sonar:sonar')
        goals('-Psonar-ci-cosmic-microservices,production')
        goals("-Dci.sonar-runner.password=\"${injectJobVariable("SONAR_RUNNER_PASSWORD")}\"")
        goals("-Dcosmic-microservices.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
        goals("-DskipITs")
        goals("-Dsonar.branch=${injectJobVariable(GIT_REPO_BRANCH_PARAM)}-${isDevFolder ? 'DEV-' : ''}build")
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
