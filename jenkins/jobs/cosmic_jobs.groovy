import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCredentials
import jenkins.model.Jenkins
import org.kohsuke.github.GitHub

def DEFAULT_GIT_REPO_BRANCH = 'remotes/origin/pr/*/head'

def DEFAULT_GIT_REPO_BRANCH_PARAM = 'sha1'
def CUSTOM_WORKSPACE_PARAM        = 'CUSTOM_WORKSPACE'
def COSMIC_DIRECTORY_PARAM        = 'COSMIC_DIRECTORY'

def WORKSPACE_VAR = '${WORKSPACE}'

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORGANIZATION_REPO_NAME           = 'organization_utils'
def ORGANIZATION_NAME                = 'MissionCriticalCloud'
def ORG_UTILS_GITHUB_REPOSITORY      = "${ORGANIZATION_NAME}/${ORGANIZATION_REPO_NAME}"
def COSMIC_GITHUB_REPOSITORY         = "${ORGANIZATION_NAME}/cosmic"
def PACKAGING_GITHUB_REPOSITORY      = "${ORGANIZATION_NAME}/packaging"
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_REPOSITORY_NAME_PARAM     = 'githubRepository'
def GITHUB_OAUTH2_CREDENTIAL_PARAM   = 'mccdJenkinsOauth2'
def SONAR_RUNNER_PASSWORD_PARAM      = 'sonarRunnerPassword'
def ARTEFACTS_TO_ARCHIVE_PARAM       = 'artefactsToArchive'
def REQUIRED_HARDWARE_PARAM          = 'requiredHardware'
def TESTS_PARAM                      = 'tests'

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'

def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'
def SONAR_RUNNER_PASSOWRD_CREDENTIALS     = 'df77a17c-5613-4fdf-8c49-52789b613e51'

def DEFAULT_MARVIN_CONFIG_FILE = '/data/shared/marvin/mct-zone1-kvm1-kvm2.cfg'

def MAVEN_REPORTS = [
  '**/target/surefire-reports/*.xml',
  '**/target/failsafe-reports/*.xml'
]

def MARVIN_REPORTS = [
  'nosetests-required_hardware*'
]

def XUNIT_REPORTS = MARVIN_REPORTS + MAVEN_REPORTS

def COSMIC_PACKAGING_ARTEFACTS = [
  'dist/rpmbuild/RPMS/x86_64/cosmic-*.rpm'
]

def COSMIC_TEST_ARTEFACTS = [
  'nosetests-required_hardware*',
  'MarvinLogs/'
]

def CLEAN_UP_JOB_ARTIFACTS = [
  'cs1-management-logs/',
  'kvm1-agent-logs/',
  'kvm2-agent-logs/'
]

def COSMIC_BUILD_ARTEFACTS = [
  'cosmic-client/copy-from-cosmic-core/db/db/',
  'cosmic-client/copy-from-cosmic-core/db/create-*.sql',
  'cosmic-client/copy-from-cosmic-core/db/templates*.sql',
  'cosmic-client/copy-from-cosmic-core/scripts/storage/secondary/',
  'cosmic-client/target/cloud-client-ui-*.war',
  'cosmic-client/target/conf/',
  'cosmic-client/target/pythonlibs/',
  'cosmic-client/target/utilities/',
  'cosmic-core/developer/developer-prefill.sql',
  'cosmic-core/test/integration/',
  'cosmic-core/**/target/*.jar',
  'cosmic-core/tools/marvin/dist/Marvin-*.tar.gz',
  'cosmic-plugin-hypervisor-kvm/target/*.jar',
  'cosmic-plugin-hypervisor-ovm3/target/*.jar',
  'cosmic-plugin-hypervisor-xenserver/target/*.jar'
] + COSMIC_PACKAGING_ARTEFACTS + COSMIC_TEST_ARTEFACTS + CLEAN_UP_JOB_ARTIFACTS

def COSMIC_TESTS_WITH_HARDWARE = [
  'smoke/test_password_server.py',
  'smoke/test_vpc_redundant.py',
  'smoke/test_routers_iptables_default_policy.py',
  'smoke/test_routers_network_ops.py',
  'smoke/test_vpc_router_nics.py',
  'smoke/test_router_dhcphosts.py',
  'smoke/test_loadbalance.py',
  'smoke/test_internal_lb.py',
  'smoke/test_ssvm.py',
  'smoke/test_vpc_vpn.py',
  'smoke/test_privategw_acl.py',
  'smoke/test_network.py'
]

def COSMIC_TESTS_WITHOUT_HARDWARE = [
  'smoke/test_routers.py',
  'smoke/test_network_acl.py',
  'smoke/test_reset_vm_on_reboot.py',
  'smoke/test_vm_life_cycle.py',
  'smoke/test_service_offerings.py',
  'smoke/test_network.py',
  'component/test_vpc_offerings.py',
  'component/test_vpc_routers.py'
]

// dev folder is to play arround with jobs.
// Jobs defined there should never autmatically trigger
def FOLDERS = [
  'cosmic',
  'cosmic-dev'
]

def DEFAULT_EXECUTOR     = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

FOLDERS.each { folderName ->
  def seedJob                             = "${folderName}/seed-job"
  def trackingRepoUpdate                  = "${folderName}/tracking-repo-update"
  def mavenBuild                          = "${folderName}/maven-build"
  def mavenSonarBuild                     = "${folderName}/maven-sonar-buid"
  def trackingRepoPullRequestBuild        = "${folderName}/tracking-repo-pull-request-build"
  def trackingRepoMasterBuild             = "${folderName}/tracking-repo-master-build"
  def trackingRepoBuild                   = "${folderName}/tracking-repo-build"
  def trackingRepoBuildAndPackageJob      = "${folderName}/tracking-repo-build-and-package"
  def packageCosmicJob                    = "${folderName}/rpm-package"
  def prepareInfraForIntegrationTests     = "${folderName}/prepare-infrastructure-for-integration-tests"
  def setupInfraForIntegrationTests       = "${folderName}/setup-infrastructure-for-integration-tests"
  def deployDatacenterForIntegrationTests = "${folderName}/deploy-datacenter-for-integration-tests"
  def runIntegrationTestsWithHardware     = "${folderName}/run-integration-tests-with-hardware"
  def runIntegrationTestsWithoutHardware  = "${folderName}/run-integration-tests-without-hardware"
  def collectArtifactsAndCleanup          = "${folderName}/collect-artifacts-and-cleanup"
  def runIntegrationTests                 = "${folderName}/run-integration-tests"

  def isDevFolder = folderName.endsWith('-dev')
  def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')
  def shellPrefix = isDevFolder ? 'bash -x' : ''

  folder(folderName)

  // seed job is meant to trigger when this file changes in git
  freeStyleJob(seedJob) {
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    label(DEFAULT_EXECUTOR)
    if(!isDevFolder) {
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
      // place a gradle build file that copies required dependencies into workspace
      shell(makeMultiline([
        'echo "defaultTasks \'libs\'',
        'repositories {',
        '  jcenter()',
        '}',
        'configurations {',
        '  libs',
        '}',
        'dependencies {',
        '  libs \'org.kohsuke:github-api:1.70\'',
        '}',
        'task clean(type: Delete) {',
        '  delete \'lib\'',
        '}',
        'task libs(type: Copy) {',
        '  into \'lib\'',
        '  from configurations.libs',
        '}',
        'task wrapper(type: Wrapper) {',
        '  gradleVersion = \'2.2.1\'',
        '}" > build.gradle',
        '/usr/local/gradle/bin/gradle libs'
      ]))
      dsl {
        if(!isDevFolder) {
          external('jenkins/jobs/cosmic_jobs.groovy')
        }
        additionalClasspath('lib/*')
      }
    }
  }

  if(!isDevFolder) {
    freeStyleJob(trackingRepoUpdate) {
      displayName('Cosmic tracking repo update')
      parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
      }
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
      triggers {
        if (!isDevFolder) {
          cron('H/15 * * * *')
        }
      }
      scm {
        git {
          remote {
            github(COSMIC_GITHUB_REPOSITORY, 'ssh' )
            credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
          }
          branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
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
          '  git add --all',
          '  git commit -m "Update all submodules to latest HEAD"',
          '  git push origin HEAD:master',
          'fi'
        ]))
      }
    }
  }

  freeStyleJob(trackingRepoMasterBuild) {
    displayName('Cosmic master full build')
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
        branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(false)
        configure { node ->
          node / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
            includedRegions '**'
            excludedRegions ''
          }
        }
      }
    }
    steps {
      downstreamParameterized {
        trigger(trackingRepoBuild) {
          parameters {
            predefinedProp(DEFAULT_GIT_REPO_BRANCH_PARAM, 'master')
          }
          block {
            buildStepFailure('UNSTABLE')
            failure('FAILURE')
            unstable('UNSTABLE')
          }
        }
      }
    }
  }

  // Build for a branch of tracking repo
  multiJob(trackingRepoBuild) {
    displayName('Cosmic full build')
    parameters {
      stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'master', 'Branch to be built')
    }
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
    scm {
      git {
        remote {
          github(COSMIC_GITHUB_REPOSITORY, 'ssh')
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
          name('origin')
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
        }
        branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(false)
      }
    }
    steps {
      phase('Build maven project and prepare infrastructure for integrations tests') {
        phaseJob(trackingRepoBuildAndPackageJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
        phaseJob(prepareInfraForIntegrationTests) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
          }
        }
      }
      phase('Setup infrastructure for integration tests') {
        phaseJob(setupInfraForIntegrationTests) {
          currentJobParameters(false)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
      }
      shell('rm -rf /tmp/MarvinLogs/DeployDataCenter_*')
      phase('Deploy datacenter') {
        phaseJob(deployDatacenterForIntegrationTests) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
          }
        }
      }
      shell("mkdir -p MarvinLogs")
      shell('cp -rf /tmp/MarvinLogs/DeployDataCenter_* MarvinLogs/')
      shell("rm -rf /tmp/MarvinLogs/test_*")
      phase('Run integration tests') {
        continuationCondition('ALWAYS')
        phaseJob(runIntegrationTestsWithHardware) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
          }
        }
        phaseJob(runIntegrationTestsWithoutHardware) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
          }
        }
      }
      shell("${shellPrefix} /data/shared/ci/ci-collect-integration-tests-coverage.sh")
      phase('Sonar analysis') {
        phaseJob(mavenSonarBuild) {
          currentJobParameters(false)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
      }
      shell("cp -rf /tmp/MarvinLogs/test_* MarvinLogs/")
      phase('Report, Archive and Cleanup') {
        phaseJob(collectArtifactsAndCleanup) {
          currentJobParameters(false)
          parameters {
            sameNode()
            gitRevision(false)
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
      if(!isDevFolder) {
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

  // build for pull requests to tracking repo
  multiJob(trackingRepoPullRequestBuild) {
    displayName('Cosmic pull request build')
    parameters {
      stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, 'sha1', 'Branch to be built')
    }
    concurrentBuild()
    label(DEFAULT_EXECUTOR)
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
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
        }
        branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
        clean(true)
        recursiveSubmodules(true)
        trackingSubmodules(true)
      }
    }
    triggers {
      if(!isDevFolder) {
        pullRequest {
          extensions {
            triggerPhrase('go build')
            permitAll()
            useGitHubHooks()
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
    steps {
      phase('Build maven project') {
        phaseJob(mavenBuild) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
      }
    }
    publishers {
      archiveJunit(makePatternList(MAVEN_REPORTS)) {
        retainLongStdout()
        testDataPublishers {
            publishTestStabilityData()
        }
      }
      if(!isDevFolder) {
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

  // Job that builds with maven and packaging scripts
  multiJob(trackingRepoBuildAndPackageJob) {
    parameters {
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
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
    steps {
      phase('Build maven project') {
        phaseJob(mavenBuild) {
          currentJobParameters(true)
          parameters {
            sameNode()
            gitRevision(false)
          }
        }
      }
      phase('Package artefacts') {
        phaseJob(packageCosmicJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(COSMIC_DIRECTORY_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
          }
        }
      }
      copyArtifacts(packageCosmicJob) {
        includePatterns(makePatternList(COSMIC_PACKAGING_ARTEFACTS))
        fingerprintArtifacts(true)
        buildSelector {
          multiJobBuild()
        }
      }
    }
    publishers {
      archiveArtifacts {
        pattern(makePatternList(COSMIC_BUILD_ARTEFACTS))
        onlyIfSuccessful()
      }
      if(!isDevFolder) {
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

  freeStyleJob(runIntegrationTestsWithHardware) {
    parameters {
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
      textParam(TESTS_PARAM, makeMultiline(isDevFolder ? subArray(COSMIC_TESTS_WITH_HARDWARE) : COSMIC_TESTS_WITH_HARDWARE), 'Set of Marvin tests to execute')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(executorLabelMct)
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    concurrentBuild()
    throttleConcurrentBuilds {
      maxPerNode(1)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    steps {
      downstreamParameterized {
        trigger(runIntegrationTests) {
          block {
            buildStepFailure('never')
            failure('never')
            unstable('never')
          }
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
            booleanParam(REQUIRED_HARDWARE_PARAM, true)
            predefinedProp(TESTS_PARAM, injectJobVariable(TESTS_PARAM))
          }
        }
      }
    }
    publishers{
      if(!isDevFolder) {
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

  freeStyleJob(runIntegrationTestsWithoutHardware) {
    parameters {
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
      textParam(TESTS_PARAM, makeMultiline(isDevFolder ? subArray(COSMIC_TESTS_WITHOUT_HARDWARE) : COSMIC_TESTS_WITHOUT_HARDWARE), 'Set of Marvin tests to execute')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(executorLabelMct)
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    concurrentBuild()
    throttleConcurrentBuilds {
      maxPerNode(1)
    }
    wrappers {
      colorizeOutput('xterm')
      timestamps()
    }
    steps {
      downstreamParameterized {
        trigger(runIntegrationTests) {
          block {
            buildStepFailure('never')
            failure('never')
            unstable('never')
          }
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(false)
            booleanParam(REQUIRED_HARDWARE_PARAM, false)
            predefinedProp(TESTS_PARAM, injectJobVariable(TESTS_PARAM))
          }
        }
      }
    }
    publishers {
      if(!isDevFolder) {
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
      shell("${shellPrefix} /data/shared/ci/ci-cleanup.sh -m /data/shared/marvin/mct-zone1-kvm1-kvm2.cfg")
    }
    publishers {
      archiveArtifacts {
        pattern(makePatternList(CLEAN_UP_JOB_ARTIFACTS))
      }
      if(!isDevFolder) {
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
    environmentVariables {
      env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
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
    goals('-Pdeveloper')
    goals('-Psystemvm')
    goals('-Psonar-ci-cosmic')
    goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
    publishers {
      if(!isDevFolder) {
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

  mavenJob(mavenSonarBuild) {
    parameters {
      credentialsParam(SONAR_RUNNER_PASSWORD_PARAM) {
        type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
        required()
        defaultValue(SONAR_RUNNER_PASSOWRD_CREDENTIALS)
        description('sonar-runner user credentials')
      }
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
    }
    environmentVariables {
      env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
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
          usernamePassword('SONAR_RUNNER_USERNAME', 'SONAR_RUNNER_PASSWORD', 'SONAR_RUNNER_PASSWORD_PARAM')
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    archivingDisabled(true)
    concurrentBuild(true)
    goals('sonar:sonar')
    goals('-Psonar-ci-cosmic')
    goals("-Dci.sonar-runner.password=\"${injectJobVariable("SONAR_RUNNER_PASSWORD")}\"")
    goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
    goals("-DskipITs")
    goals("-Dsonar.branch=${isDevFolder ? 'development' : 'production'}-build")
    publishers {
      if(!isDevFolder) {
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

  freeStyleJob(packageCosmicJob) {
    parameters {
      stringParam(COSMIC_DIRECTORY_PARAM, WORKSPACE_VAR, 'A directory with the cosmic sources and artefacts to use for the job')
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
    scm {
      git {
        remote {
          github(PACKAGING_GITHUB_REPOSITORY, 'ssh' )
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
        }
        branch(DEFAULT_GITHUB_REPOSITORY_BRANCH)
        shallowClone(true)
        clean(true)
      }
    }
    steps {
      shell("${shellPrefix} ./package_cosmic.sh  -d centos7 -f ${injectJobVariable(COSMIC_DIRECTORY_PARAM)}")
    }
    publishers {
      archiveArtifacts {
        pattern(makePatternList(COSMIC_PACKAGING_ARTEFACTS))
        onlyIfSuccessful()
      }
      if(!isDevFolder) {
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
    publishers {
      if(!isDevFolder) {
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

  // Job that prepares the infrastructure for the cosmic integration tests
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
    publishers {
      if(!isDevFolder) {
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
      shell("${shellPrefix} /data/shared/ci/ci-deploy-data-center.sh -m ${DEFAULT_MARVIN_CONFIG_FILE}")
    }
    publishers {
      if(!isDevFolder) {
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

  freeStyleJob(runIntegrationTests) {
    parameters {
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
      booleanParam(REQUIRED_HARDWARE_PARAM, false, 'Flag passed to Marvin to select test cases to execute')
      textParam(TESTS_PARAM, '', 'Set of Marvin tests to execute')
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    label(executorLabelMct)
    concurrentBuild()
    throttleConcurrentBuilds {
      maxPerNode(2) // there will be two test runs in parallel (with/without hardware)
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
    publishers {
      if(!isDevFolder) {
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

  def pluginRepositories = isDevFolder ? getFakeRepos() : getPluginRepositories(ORGANIZATION_NAME, DEFAULT_GITHUB_USER_NAME);
  // Pull request jobs for plugins
  pluginRepositories.each { cosmicRepo ->
    def targetBranch = injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM)
    def repoName = cosmicRepo.getName()
    def githubRepository = "${ORGANIZATION_NAME}/" + repoName
    def repoJobName =  "${folderName}/plugin-pull-request-build-${repoName}"

    // job to build cosmic a plugin
    multiJob(repoJobName) {
      displayName('Plugin pull request build: ' + repoName)
      parameters {
        stringParam(DEFAULT_GIT_REPO_BRANCH_PARAM, DEFAULT_GIT_REPO_BRANCH, 'Branch to be built')
      }
      concurrentBuild()
      label(DEFAULT_EXECUTOR)
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
            github(githubRepository, 'ssh')
            credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
            name('origin')
            refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
          }
          branch(injectJobVariable(DEFAULT_GIT_REPO_BRANCH_PARAM))
          clean(true)
        }
      }
      triggers {
        if(!isDevFolder) {
          pullRequest {
            extensions {
              triggerPhrase('go build')
              permitAll()
              useGitHubHooks()
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
      steps {
        phase('Build maven project') {
          phaseJob(mavenBuild) {
            currentJobParameters(true)
            parameters {
              predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
              sameNode()
              gitRevision(false)
            }
          }
        }
      }
      publishers {
        if(repoName != 'cosmic-client') {
          archiveJunit(makePatternList(MAVEN_REPORTS)) {
            retainLongStdout(true)
            testDataPublishers {
                publishTestStabilityData()
            }
          }
        }
        if(!isDevFolder) {
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
  }
}

def getPluginRepositories(githubOrganizatioName, githubUserName) {
  def githubCredentials = CredentialsProvider.lookupCredentials(
    StandardCredentials.class,
    Jenkins.instance
  )
  gitubUserCredentials = githubCredentials.find({ it.description.contains(githubUserName) && it.description.contains('OAuth2') })

  def github = GitHub.connect(githubUserName, gitubUserCredentials.secret.getPlainText())

  def organization = github.getOrganization(githubOrganizatioName);
  def repos = organization.listRepositories().toList();

  return repos.findAll { it.getName().startsWith('cosmic-') }
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

def getFakeRepos() { return [ new FakeRepo('cosmic-client'), new FakeRepo('cosmic-core') ] }

// used for testing in order to avoind calling github api all the time
class FakeRepo {

  private String name;

  public FakeRepo(String name) {
    this.name = name;
  }

  public String getName() { return name }
}
