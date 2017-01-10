def GIT_REPO_BRANCH_PARAM = 'branch'
def CUSTOM_WORKSPACE_PARAM = 'CUSTOM_WORKSPACE'

def WORKSPACE_VAR = '${WORKSPACE}'

def ORGANIZATION_REPO_NAME = 'organization_utils'
def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORG_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_REPO_NAME}"
def COSMIC_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/cosmic"
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_OAUTH2_CREDENTIAL_PARAM = 'mccdJenkinsOauth2'
def SONAR_RUNNER_PASSWORD_PARAM = 'sonarRunnerPassword'
def REQUIRED_HARDWARE_PARAM = 'requiredHardware'
def TESTS_PARAM = 'tests'

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

def DEFAULT_MARVIN_CONFIG_FILE = '/data/shared/marvin/mct-zone1-cs1-kvm1-kvm2.cfg'
def MARVIN_VERSION_FILE = 'cosmic-marvin/setup.py'

def MANAGEMENT_SERVER_LOG_FILE = '/var/log/cosmic/management/management.log'
def MANAGEMENT_SERVER_LOG_ROTATION = '/var/log/cosmic/management/management-%d{yyyy-MM-dd}.log.gz'

def MAVEN_REPORTS = [
        '**/target/surefire-reports/*.xml'
]

def MARVIN_REPORTS = [
        'nosetests-required_hardware*'
]

def MARVIN_DEPLOY_DC_LOGS = [
        'MarvinLogs/dc_entries_*.obj',
        'MarvinLogs/deployDc_failed_plus_exceptions.txt',
        'MarvinLogs/deployDc_runinfo.txt'
]

def XUNIT_REPORTS = MARVIN_REPORTS + MAVEN_REPORTS

def COSMIC_TEST_ARTEFACTS = [
        'MarvinLogs/'
] + MARVIN_REPORTS

def CLEAN_UP_JOB_ARTIFACTS = [
        'cs1-management-logs/',
        'kvm1-agent-logs/',
        'kvm2-agent-logs/'
]

def COSMIC_BUILD_ARTEFACTS = [
        'cosmic-client/target/setup/',
        'cosmic-client/target/cloud-client-ui-*.war',
        'cosmic-client/target/conf/',
        'cosmic-client/target/pythonlibs/',
        'cosmic-client/target/utilities/',
        'cosmic-core/test/integration/',
        'cosmic-core/**/target/*.jar',
        'cosmic-plugin-hypervisor-kvm/target/*.jar',
        'cosmic-plugin-hypervisor-ovm3/target/*.jar',
        'cosmic-plugin-hypervisor-xenserver/target/*.jar'
] + COSMIC_TEST_ARTEFACTS + CLEAN_UP_JOB_ARTIFACTS

def COSMIC_TESTS_WITH_HARDWARE = [
        'smoke/test_network.py',
        'smoke/test_routers_iptables_default_policy.py',
        'smoke/test_password_server.py',
        'smoke/test_vpc_redundant.py',
        'smoke/test_routers_network_ops.py',
        'smoke/test_vpc_router_nics.py',
        'smoke/test_router_dhcphosts.py',
        'smoke/test_loadbalance.py',
        'smoke/test_privategw_acl.py',
        'smoke/test_ssvm.py',
        'smoke/test_vpc_vpn.py'
]

def DEFAULT_EXECUTOR = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

// dev folder is to play with jobs.
// Jobs defined there should never automatically trigger
def FOLDERS = [
        'cosmic',
        'cosmic-dev'
]

FOLDERS.each { folderName ->

    def cosmicView = "Cosmic"

    def cosmicMasterBuild = "0001-cosmic-master-build"
    def cosmicPullRequestBuild = "0002-cosmic-pull-request-build"
    def cosmicReleaseBuild = "0003-cosmic-release-build"
    def cosmicSnapshotBuild = "0004-cosmic-snapshot-build"

    def fullBuild = "${folderName}/0020-full-build"
    def prepareInfraForIntegrationTests = "${folderName}/0200-prepare-infrastructure-for-integration-tests"
    def setupInfraForIntegrationTests = "${folderName}/0300-setup-infrastructure-for-integration-tests"
    def deployDatacenterForIntegrationTests = "${folderName}/0400-deploy-datacenter-for-integration-tests"
    def runIntegrationTests = "${folderName}/0500-run-integration-tests"
    def collectArtifactsAndCleanup = "${folderName}/0600-collect-artifacts-and-cleanup"
    def mavenBuild = "${folderName}/9997-maven-build"
    def mavenSonarBuild = "${folderName}/9998-maven-sonar-build"
    def seedJob = "${folderName}/9999-seed-job"

    def isDevFolder = folderName.endsWith('-dev')
    def shellPrefix = isDevFolder ? 'bash -x' : ''
    def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')

    folder(folderName) {
        primaryView(cosmicView)
    }

    listView("${folderName}/${cosmicView}") {
        description('Cosmic build and release jobs.')
        jobs {
            name(cosmicMasterBuild)
            name(cosmicPullRequestBuild)
            name(cosmicReleaseBuild)
            name(cosmicSnapshotBuild)
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
                                includedRegions 'jenkins/jobs/cosmic_jobs[.]groovy'
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
                        external('jenkins/jobs/cosmic_jobs.groovy')
                    }
                }
            }
        }

        multiJob("${folderName}/" + cosmicMasterBuild) {
            parameters {
                textParam(TESTS_PARAM, makeMultiline(isDevFolder ? subArray(COSMIC_TESTS_WITH_HARDWARE) : COSMIC_TESTS_WITH_HARDWARE), 'Set of integration tests to execute')
            }
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
                            predefinedProp(TESTS_PARAM, injectJobVariable(TESTS_PARAM))
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

        multiJob("${folderName}/" + cosmicPullRequestBuild) {
            parameters {
                stringParam(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_PR_BRANCH_ENV_VARIABLE_NAME), 'Branch to be built')
                textParam(TESTS_PARAM, makeMultiline(isDevFolder ? subArray(COSMIC_TESTS_WITH_HARDWARE) : COSMIC_TESTS_WITH_HARDWARE), 'Set of integration tests to execute')
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
                            predefinedProp(TESTS_PARAM, injectJobVariable(TESTS_PARAM))
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

        mavenJob("${folderName}/" + cosmicReleaseBuild) {
            parameters {
                stringParam(MAVEN_RELEASE_VERSION_PARAM, "", 'Release version')
            }
            concurrentBuild(false)
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
            preBuildSteps {
                shell("git checkout master")
                shell("sed -i \"s/VERSION = .*/VERSION = '${injectJobVariable(MAVEN_RELEASE_VERSION_PARAM)}'/g\" ${MARVIN_VERSION_FILE}")
                shell("git add ${MARVIN_VERSION_FILE}")
            }
            goals("release:prepare release:perform -Psystemvm -DreleaseVersion=${injectJobVariable(MAVEN_RELEASE_VERSION_PARAM)} ${(isDevFolder ? MAVEN_RELEASE_NO_PUSH : '')}")
        }

        mavenJob("${folderName}/" + cosmicSnapshotBuild) {
            parameters {
                stringParam(MAVEN_SNAPSHOT_VERSION_PARAM, "", 'Snapshot version, include \'-SNAPSHOT\' in the version string. Example: 6.0.0.0-SNAPSHOT')
            }
            concurrentBuild(false)
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
            preBuildSteps {
                shell("git checkout master")
            }
            goals("release:update-versions --batch-mode -DdevelopmentVersion=${injectJobVariable(MAVEN_SNAPSHOT_VERSION_PARAM)} -Psystemvm")
            postBuildSteps {
                shell("sed -i \"s/VERSION = .*/VERSION = '${injectJobVariable(MAVEN_SNAPSHOT_VERSION_PARAM)}'/g\" ${MARVIN_VERSION_FILE}")
                shell("git add .")
                shell("git commit -m \"Update SNAPSHOT version to ${injectJobVariable(MAVEN_SNAPSHOT_VERSION_PARAM)}\"")
                shell("git push origin HEAD:master")
            }
        }

        // Build for a branch of cosmic
        multiJob(fullBuild) {
            parameters {
                stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
                stringParam(GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
                textParam(TESTS_PARAM, makeMultiline(isDevFolder ? subArray(COSMIC_TESTS_WITH_HARDWARE) : COSMIC_TESTS_WITH_HARDWARE), 'Set of integration tests to execute')
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
                            sameNode()
                            gitRevision(true)
                        }
                    }
                    phaseJob(prepareInfraForIntegrationTests) {
                        currentJobParameters(false)
                        parameters {
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
                phase('Setup infrastructure for integration tests') {
                    phaseJob(setupInfraForIntegrationTests) {
                        currentJobParameters(false)
                        parameters {
                            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
                shell('rm -rf MarvinLogs')
                phase('Deploy datacenter') {
                    phaseJob(deployDatacenterForIntegrationTests) {
                        currentJobParameters(false)
                        parameters {
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
                copyArtifacts(deployDatacenterForIntegrationTests) {
                    includePatterns(makePatternList(MARVIN_DEPLOY_DC_LOGS))
                    fingerprintArtifacts(true)
                    buildSelector {
                        multiJobBuild()
                    }
                }
                phase('Run integration tests') {
                    continuationCondition('ALWAYS')
                    phaseJob(runIntegrationTests) {
                        currentJobParameters(false)
                        parameters {
                            sameNode()
                            gitRevision(true)
                            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
                            predefinedProp(TESTS_PARAM, injectJobVariable(TESTS_PARAM))
                        }
                    }
                }
                shell('mkdir -p MarvinLogs')
                shell('mv cosmic-core/test/integration/runinfo.txt MarvinLogs/tests_runinfo.txt')
                shell('mv cosmic-core/test/integration/failed_plus_exceptions.txt MarvinLogs/tests_failed_plus_exceptions.txt')
                shell("${shellPrefix} /data/shared/ci/ci-collect-integration-tests-coverage.sh -m /data/shared/marvin/mct-zone1-cs1-kvm1-kvm2.cfg")
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
                phase('Report, Archive and Cleanup') {
                    phaseJob(collectArtifactsAndCleanup) {
                        currentJobParameters(false)
                        parameters {
                            sameNode()
                            gitRevision(true)
                        }
                    }
                }
                copyArtifacts(collectArtifactsAndCleanup) {
                    includePatterns(makePatternList(CLEAN_UP_JOB_ARTIFACTS))
                    fingerprintArtifacts(true)
                    buildSelector {
                        multiJobBuild()
                    }
                }
            }
        }
    }

    freeStyleJob(collectArtifactsAndCleanup) {
        label(executorLabelMct)
        concurrentBuild()
        throttleConcurrentBuilds {
            maxPerNode(1)
        }
        logRotator {
            numToKeep(5)
            artifactNumToKeep(5)
        }
        wrappers {
            colorizeOutput('xterm')
            timestamps()
        }
        steps {
            shell('rm -rf ./*')
            shell("${shellPrefix} /data/shared/ci/ci-cleanup.sh -m /data/shared/marvin/mct-zone1-cs1-kvm1-kvm2.cfg")
        }
        publishers {
            archiveArtifacts {
                pattern(makePatternList(CLEAN_UP_JOB_ARTIFACTS))
            }
        }
    }

    // Job that prepares the infrastructure for the cosmic integration tests
    freeStyleJob(prepareInfraForIntegrationTests) {
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
            shell('rm -rf ./*')
            shell("${shellPrefix} /data/shared/ci/ci-prepare-infra.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
        }
    }

    // Job that prepares the infrastructure for the cosmic integration tests
    freeStyleJob(prepareInfraForIntegrationTests) {
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
            shell('rm -rf ./*')
            shell("${shellPrefix} /data/shared/ci/ci-prepare-infra.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
        }
    }

    // Job that deploys and configures build artefacts for the cosmic integration tests
    freeStyleJob(setupInfraForIntegrationTests) {
        parameters {
            stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
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
            shell("${shellPrefix} /data/shared/ci/ci-setup-infra.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
        }
    }

    freeStyleJob(deployDatacenterForIntegrationTests) {
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
            shell('rm -rf *')
            shell("${shellPrefix} /data/shared/ci/ci-deploy-data-center.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
            shell('mkdir MarvinLogs')
            shell('mv dc_entries_*.obj MarvinLogs/')
            shell('mv runinfo.txt MarvinLogs/deployDc_runinfo.txt')
            shell('mv failed_plus_exceptions.txt MarvinLogs/deployDc_failed_plus_exceptions.txt')
        }
        publishers {
            archiveArtifacts {
                pattern(makePatternList(MARVIN_DEPLOY_DC_LOGS))
                onlyIfSuccessful(false)
            }
        }
    }

    freeStyleJob(runIntegrationTests) {
        parameters {
            stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
            booleanParam(REQUIRED_HARDWARE_PARAM, true, 'Flag passed to Marvin to select test cases to execute')
            textParam(TESTS_PARAM, '', 'Set of Marvin tests to execute')
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
            shell("${shellPrefix} /data/shared/ci/ci-run-marvin-tests.sh -m ${DEFAULT_MARVIN_CONFIG_FILE} -h ${injectJobVariable(REQUIRED_HARDWARE_PARAM)} ${injectJobVariable(flattenLines(TESTS_PARAM))} || true")
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
        goals('install')
        if (!isDevFolder) {
            goals('deploy')
        }
        goals('-U')
        goals('-Psystemvm')
        goals('-Psonar-ci-cosmic')
        goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
        goals("-Dlog.file.management.server=\"${MANAGEMENT_SERVER_LOG_FILE}\"")
        goals("-Dlog.rotation.management.server=\"${MANAGEMENT_SERVER_LOG_ROTATION}\"")
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
        customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
        archivingDisabled(true)
        goals('org.jacoco:jacoco-maven-plugin:merge@merge-integration-test-coverage')
        goals('sonar:sonar')
        goals('-Psonar-ci-cosmic')
        goals("-Dci.sonar-runner.password=\"${injectJobVariable("SONAR_RUNNER_PASSWORD")}\"")
        goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
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

def makeMultiline(lines) {
    return listToStringWithSeparator('\n', lines)
}

def makeSpaceSeperatedList(elements) {
    return listToStringWithSeparator(' ', elements)
}

def listToStringWithSeparator(separator, list) {
    return list.join(separator)
}

def flattenLines(lines) {
    return lines.replaceAll('\n', ' ')
}

def subArray(array) {
    def endIndex = array.size() > 1 ? 1 : 0
    return array[0..endIndex]
}

