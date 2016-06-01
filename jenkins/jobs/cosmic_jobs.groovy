import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCredentials
import jenkins.model.Jenkins
import org.kohsuke.github.GitHub

def DEFAULT_GIT_REPO_BRANCH = 'remotes/origin/pr/*/head'

def GIT_REPO_BRANCH_PARAM    = 'branch'
def CUSTOM_WORKSPACE_PARAM   = 'CUSTOM_WORKSPACE'
def COSMIC_DIRECTORY_PARAM   = 'COSMIC_DIRECTORY'

def WORKSPACE_VAR = '${WORKSPACE}'

def DEFAULT_GITHUB_USER_NAME         = 'mccd-jenkins'
def ORGANIZATION_REPO_NAME           = 'organization_utils'
def ORGANIZATION_NAME                = 'MissionCriticalCloud'
def ORG_UTILS_GITHUB_REPOSITORY      = "${ORGANIZATION_NAME}/${ORGANIZATION_REPO_NAME}"
def COSMIC_GITHUB_REPOSITORY         = "${ORGANIZATION_NAME}/cosmic"
def PACKAGING_GITHUB_REPOSITORY      = "${ORGANIZATION_NAME}/packaging"
def DEFAULT_GITHUB_REPOSITORY_BRANCH = 'master'

def GITHUB_REPOSITORY_NAME_PARAM      = 'githubRepository'
def GITHUB_OAUTH2_CREDENTIAL_PARAM    = 'mccdJenkinsOauth2'
def SONAR_RUNNER_PASSWORD_PARAM       = 'sonarRunnerPassword'
def ARTEFACTS_TO_ARCHIVE_PARAM        = 'artefactsToArchive'
def REQUIRED_HARDWARE_PARAM           = 'requiredHardware'
def TESTS_PARAM                       = 'tests'
def MAVEN_EXTRA_GOALS_PARAM           = 'mvnExtraGoals'
def MAVEN_RELEASE_VERSION_PARAM       = 'releaseVersion'

def TOP_LEVEL_COSMIC_JOBS_CATEGORY = 'top-level-cosmic-jobs'

def GITHUB_OAUTH2_TOKEN_ENV_VAR   = 'MCCD_JENKINS_OAUTH2_TOKEN'
def MAVEN_RELEASE_VERSION_ENV_VAR = 'releaseVersion'
def MAVEN_OPTIONS_ENV_VAR         = 'MAVEN_OPTS'

def MAVEN_OPTIONS_RELEASE_JOB = '-Xmx2048m -Xms2048m'

def MAVEN_RELEASE_NO_SUBMODULES           = '-N -Darguments=-N'
def MAVEN_RELEASE_AUTO_VERSION_SUBMODULES = '-DautoVersionSubmodules=true'
def MAVEN_RELEASE_NO_PUSH                 = '-DpushChanges=false -DlocalCheckout=true'

def GIT_BRANCH_ENV_VARIABLE_NAME = 'GIT_BRANCH'
def GIT_PR_BRANCH_ENV_VARIABLE_NAME = 'ghprbActualCommit'

def DEFAULT_GITHUB_JOB_LABEL = 'mccd jenkins build'

def MCCD_JENKINS_GITHUB_CREDENTIALS       = 'f4ec9d6e-49fb-497c-bd1f-e42d88e105da'
def MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS = '95c201f6-794e-434b-a667-cf079aac4dfc'
def SONAR_RUNNER_PASSOWRD_CREDENTIALS     = 'df77a17c-5613-4fdf-8c49-52789b613e51'

def DEFAULT_MARVIN_CONFIG_FILE = '/data/shared/marvin/mct-zone1-kvm1-kvm2.cfg'

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

def COSMIC_PACKAGING_ARTEFACTS = [
  'dist/rpmbuild/RPMS/x86_64/cosmic-*.rpm'
]

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
  'cosmic-core/developer/developer-prefill.sql',
  'cosmic-core/test/integration/',
  'cosmic-core/**/target/*.jar',
  'cosmic-plugin-hypervisor-kvm/target/*.jar',
  'cosmic-plugin-hypervisor-ovm3/target/*.jar',
  'cosmic-plugin-hypervisor-xenserver/target/*.jar'
] + COSMIC_PACKAGING_ARTEFACTS + COSMIC_TEST_ARTEFACTS + CLEAN_UP_JOB_ARTIFACTS

def COSMIC_TESTS_WITH_HARDWARE = [
  'smoke/test_network.py',
  'smoke/test_routers_iptables_default_policy.py',
  'smoke/test_password_server.py',
  'smoke/test_vpc_redundant.py',
  'smoke/test_routers_network_ops.py',
  'smoke/test_vpc_router_nics.py',
  'smoke/test_router_dhcphosts.py',
  'smoke/test_loadbalance.py',
  'smoke/test_internal_lb.py',
  'smoke/test_ssvm.py',
  'smoke/test_vpc_vpn.py'
]

def DEFAULT_EXECUTOR     = 'executor'
def DEFAULT_EXECUTOR_MCT = 'executor-mct'

// dev folder is to play arround with jobs.
// Jobs defined there should never autmatically trigger
def FOLDERS = [
  'cosmic',
  'cosmic-dev'
]

FOLDERS.each { folderName ->
  def trackingRepoUpdate                              = "${folderName}/0000-tracking-repo-update"
  def trackingRepoMasterBuild                         = "${folderName}/0001-tracking-repo-master-build"
  def trackingRepoBranchBuild                         = "${folderName}/0002-tracking-repo-branch-build"
  def trackingRepoPullRequestBuild                    = "${folderName}/0003-tracking-repo-pull-request-build"
  def trackingRepoReleaseBuild                        = "${folderName}/0004-tracking-repo-release-build"

  def job_name_counter = 5 // this will be used to index plugin pr jobs, it should follow from the last number in the above line

  def trackingRepoBuild                               = "${folderName}/0020-tracking-repo-build"
  def trackingRepoBuildAndPackageJob                  = "${folderName}/0100-tracking-repo-build-and-package"
  def packageCosmicJob                                = "${folderName}/1000-rpm-package"
  def prepareInfraForIntegrationTests                 = "${folderName}/0200-prepare-infrastructure-for-integration-tests"
  def setupInfraForIntegrationTests                   = "${folderName}/0300-setup-infrastructure-for-integration-tests"
  def deployDatacenterForIntegrationTests             = "${folderName}/0400-deploy-datacenter-for-integration-tests"
  def runIntegrationTests                             = "${folderName}/0500-run-integration-tests"
  def collectArtifactsAndCleanup                      = "${folderName}/0600-collect-artifacts-and-cleanup"
  def seedJob                                         = "${folderName}/9991-seed-job"
  def mavenReleaseUpdateDependenciesToNextSnapshot    = "${folderName}/9993-maven-versions-update-dependencies-next-snapshot"
  def mavenReleaseUpdateDependenciesToReleaseVersions = "${folderName}/9994-maven-versions-update-dependencies-release-version"
  def mavenPluginRelease                              = "${folderName}/9995-maven-release-build"
  def mavenVersionsUpdateParent                       = "${folderName}/9996-maven-versions-update-parent"
  def mavenRelease                                    = "${folderName}/9997-maven-release-build"
  def mavenBuild                                      = "${folderName}/9998-maven-build"
  def mavenSonarBuild                                 = "${folderName}/9999-maven-sonar-build"

  def isDevFolder = folderName.endsWith('-dev')
  def shellPrefix = isDevFolder ? 'bash -x' : ''
  def executorLabelMct = DEFAULT_EXECUTOR_MCT + (isDevFolder ? '-dev' : '')

  def helperJobsFolder = 'helper_jobs' + (isDevFolder ? '_dev' : '')

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
          extensions {
            cleanAfterCheckout()
            cleanBeforeCheckout()
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
          extensions {
            cleanAfterCheckout()
            cleanBeforeCheckout()
            wipeOutWorkspace()
          }
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

  multiJob(trackingRepoMasterBuild) {
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
          cleanAfterCheckout()
          cleanBeforeCheckout()
          wipeOutWorkspace()
        }
        recursiveSubmodules(true)
        trackingSubmodules(false)
      }
    }
    steps {
      phase('Full Build') {
        phaseJob(trackingRepoBuild) {
          parameters {
            sameNode()
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            predefinedProp(GIT_REPO_BRANCH_PARAM, 'master')
            gitRevision(true)
          }
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

  multiJob(trackingRepoBranchBuild) {
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
          refspec('+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*')
        }
        branch('origin/build/**')
        extensions {
          cleanAfterCheckout()
          cleanBeforeCheckout()
          wipeOutWorkspace()
        }
        recursiveSubmodules(true)
        trackingSubmodules(false)
      }
    }
    steps {
      phase('Full build') {
        phaseJob(trackingRepoBuild) {
          parameters {
            sameNode()
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            predefinedProp(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_BRANCH_ENV_VARIABLE_NAME))
            gitRevision(true)
          }
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

  multiJob(trackingRepoReleaseBuild) {
    parameters {
      stringParam(MAVEN_RELEASE_VERSION_PARAM, '', 'Custom release version (default is empty)')
    }
    blockOn(trackingRepoMasterBuild){
      blockLevel('GLOBAL')
      scanQueueFor('ALL')
    }
    label(executorLabelMct)
    concurrentBuild()
    throttleConcurrentBuilds {
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
    scm {
      git {
        remote {
          github(COSMIC_GITHUB_REPOSITORY, 'ssh')
          credentials(MCCD_JENKINS_GITHUB_CREDENTIALS)
          name('origin')
        }
        branch('master')
        extensions {
          wipeOutWorkspace()
          localBranch('master')
        }
        recursiveSubmodules(true)
        trackingSubmodules(false)
      }
    }
    steps {
      phase('Release Cosmic') {
        phaseJob(mavenRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR)
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_NO_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Update Parent in Submodules') {
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-agent")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-core")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-client")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-event-bus-rabbitmq")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-kvm")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-ovm3")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-xenserver")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-ldap")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenVersionsUpdateParent) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-sha256salted")
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Release Cosmic Core') {
        phaseJob(mavenRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-core")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Release Cosmic Plugins') {
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-agent")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-event-bus-rabbitmq")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-kvm")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-xenserver")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-ovm3")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-ldap")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-sha256salted")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Release Cosmic Client') {
        phaseJob(mavenPluginRelease) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-client")
            predefinedProp(MAVEN_EXTRA_GOALS_PARAM, MAVEN_RELEASE_AUTO_VERSION_SUBMODULES)
            predefinedProp(MAVEN_RELEASE_VERSION_PARAM, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
            sameNode()
            gitRevision(true)
          }
        }
      }
      shell('mvn deploy -N') // this will push the new parent snapshot to nexus so it can be updated in the submodules
      phase('Update dependencies in Cosmic Core') {
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-core")
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Update dependencies in submodules') {
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-agent")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-event-bus-rabbitmq")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-ovm3")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-kvm")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-hypervisor-xenserver")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-ldap")
            sameNode()
            gitRevision(true)
          }
        }
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-plugin-user-authenticator-sha256salted")
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Update dependencies in Cosmic Client') {
        phaseJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
          currentJobParameters(true)
          parameters {
            predefinedProp(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR + "/cosmic-client")
            sameNode()
            gitRevision(true)
          }
        }
      }
    }
    publishers {
      if(!isDevFolder) {
        slackNotifications {
          notifyAborted()
          notifyFailure()
          notifyNotBuilt()
          notifyUnstable()
          notifyBackToNormal()
        }
      }
    }
  }

  // Build for a branch of tracking repo
  multiJob(trackingRepoBuild) {
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
        phaseJob(trackingRepoBuildAndPackageJob) {
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
      shell("${shellPrefix} /data/shared/ci/ci-collect-integration-tests-coverage.sh")
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

  // build for pull requests to tracking repo
  multiJob(trackingRepoPullRequestBuild) {
    parameters {
      stringParam(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_PR_BRANCH_ENV_VARIABLE_NAME), 'Branch to be built')
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
        branch(injectJobVariable(GIT_REPO_BRANCH_PARAM))
        extensions {
          cleanAfterCheckout()
          cleanBeforeCheckout()
          wipeOutWorkspace()
        }
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
            gitRevision(true)
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
            gitRevision(true)
          }
        }
      }
      phase('Package artefacts') {
        phaseJob(packageCosmicJob) {
          currentJobParameters(true)
          parameters {
            predefinedProp(COSMIC_DIRECTORY_PARAM, WORKSPACE_VAR)
            sameNode()
            gitRevision(true)
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
          notifyAborted()
          notifyFailure()
          notifyNotBuilt()
          notifyUnstable()
          notifyBackToNormal()
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
        extensions {
          cleanAfterCheckout()
          cleanBeforeCheckout()
        }
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
          notifyAborted()
          notifyFailure()
          notifyNotBuilt()
          notifyUnstable()
          notifyBackToNormal()
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
          notifyAborted()
          notifyFailure()
          notifyNotBuilt()
          notifyUnstable()
          notifyBackToNormal()
          includeTestSummary()
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
    goals('deploy')
    goals('-U')
    goals('-Pdeveloper')
    goals('-Psystemvm')
    goals('-Psonar-ci-cosmic')
    goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
    publishers {
      if(!isDevFolder) {
        slackNotifications {
          notifyAborted()
          notifyFailure()
          notifyNotBuilt()
          notifyUnstable()
          includeTestSummary()
          showCommitList()
        }
      }
    }
  }

  freeStyleJob(mavenVersionsUpdateParent) {
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
    steps {
      shell(makeMultiline([
        'git checkout master',
        'mvn versions:update-parent -N',
        'git add pom.xml',
        'git commit -m "Update parent to latest release version"',
        'git clean -xdf'
      ]))
    }
  }

  // generic Maven job that builds on a folder (instead of a git repo)
  // this job is meant to be called by another job that already checked out a maven project
  freeStyleJob(mavenRelease) {
    parameters {
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
      stringParam(MAVEN_EXTRA_GOALS_PARAM, '', 'Extra goals and config for the job')
      stringParam(MAVEN_RELEASE_VERSION_PARAM, '', 'The version of the new release (empty means the build number will increment)')
    }
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
      colorizeOutput('xterm')
      timestamps()
      environmentVariables {
        env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
        env(MAVEN_OPTIONS_ENV_VAR, MAVEN_OPTIONS_RELEASE_JOB)
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    steps {
      shell("mvn -B release:prepare release:perform -Psystemvm -DreleaseVersion=${injectJobVariable(MAVEN_RELEASE_VERSION_PARAM)} ${injectJobVariable(MAVEN_EXTRA_GOALS_PARAM)} ${(isDevFolder ? MAVEN_RELEASE_NO_PUSH : '')}")
    }
  }

  multiJob(mavenPluginRelease) {
    parameters {
      credentialsParam(GITHUB_OAUTH2_CREDENTIAL_PARAM) {
        type('org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl')
        required()
        defaultValue(MCCD_JENKINS_GITHUB_OAUTH_CREDENTIALS)
        description('mccd jenkins OAuth2 token credential')
      }
      stringParam(CUSTOM_WORKSPACE_PARAM, WORKSPACE_VAR, 'A custom workspace to use for the job')
      stringParam(MAVEN_EXTRA_GOALS_PARAM, '', 'Extra goals and config for the job')
      stringParam(MAVEN_RELEASE_VERSION_PARAM, '', 'The version of the new release (empty means the build number will increment)')
    }
    logRotator {
      numToKeep(50)
      artifactNumToKeep(10)
    }
    concurrentBuild()
    wrappers {
      colorizeOutput('xterm')
      timestamps()
      environmentVariables {
        env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
        env(MAVEN_RELEASE_VERSION_ENV_VAR, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    steps {
      phase('Update dependencies to release versions') {
        phaseJob(mavenReleaseUpdateDependenciesToReleaseVersions) {
          currentJobParameters(true)
          parameters {
            sameNode()
            gitRevision(true)
          }
        }
      }
      phase('Release plugin') {
        phaseJob(mavenRelease) {
          currentJobParameters(true)
          parameters {
            sameNode()
            gitRevision(true)
          }
        }
      }
    }
  }

  freeStyleJob(mavenReleaseUpdateDependenciesToReleaseVersions) {
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
      environmentVariables {
        env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
        env(MAVEN_RELEASE_VERSION_ENV_VAR, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    steps {
      shell(makeMultiline([
        'mvn versions:force-releases',
        'if [ -z "$(git status -su -- pom.xml)" ]; then',
        '  echo "==> No dependencies changed"',
        'else',
        '  echo "==> Committing dependency changes"',
        '  git add pom.xml',
        '  git commit -m "Update dependencies to release versions"',
        '  git clean -xdf',
        'fi'
      ]))
    }
  }

  freeStyleJob(mavenReleaseUpdateDependenciesToNextSnapshot) {
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
      environmentVariables {
        env(GITHUB_OAUTH2_TOKEN_ENV_VAR, injectJobVariable(GITHUB_OAUTH2_CREDENTIAL_PARAM))
        env(MAVEN_RELEASE_VERSION_ENV_VAR, injectJobVariable(MAVEN_RELEASE_VERSION_PARAM))
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    steps {
      shell(makeMultiline([
        'mvn versions:update-parent versions:use-next-snapshots -Dinclude="cloud.cosmic.**" -DallowMinorUpdates -DallowSnapshots',
        'if [ -z "$(git status -su)" ]; then',
        '  echo "==> No dependencies changed"',
        'else',
        '  echo "==> Committing dependency changes"',
        '  git add pom.xml',
        '  git commit -m "Update parent and dependencies to next snapshot versions"',
        '  git clean -xdf',
        '  git push origin master',
        'fi',
        // Deploy so the dependent modules can fetch the latest snapshot.
        'mvn deploy -Pdeveloper -Psystemvm'
      ]))
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
      stringParam(GIT_REPO_BRANCH_PARAM, 'sha1', 'Branch to be built')
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
          usernamePassword('SONAR_RUNNER_USERNAME', 'SONAR_RUNNER_PASSWORD', injectJobVariable(SONAR_RUNNER_PASSWORD_PARAM))
      }
    }
    customWorkspace(injectJobVariable(CUSTOM_WORKSPACE_PARAM))
    archivingDisabled(true)
    goals('sonar:sonar')
    goals('-Psonar-ci-cosmic')
    goals("-Dci.sonar-runner.password=\"${injectJobVariable("SONAR_RUNNER_PASSWORD")}\"")
    goals("-Dcosmic.dir=\"${injectJobVariable(CUSTOM_WORKSPACE_PARAM)}\"")
    goals("-DskipITs")
    goals("-Dsonar.branch=${injectJobVariable(GIT_REPO_BRANCH_PARAM)}-${isDevFolder ? 'DEV-' : ''}build")
  }

  def pluginRepositories = isDevFolder ? getFakeRepos() : getPluginRepositories(ORGANIZATION_NAME, DEFAULT_GITHUB_USER_NAME);
  // Pull request jobs for plugins
  pluginRepositories.each { cosmicRepo ->
    def targetBranch = injectJobVariable(GIT_REPO_BRANCH_PARAM)
    def repoName = cosmicRepo.getName()
    def githubRepository = "${ORGANIZATION_NAME}/" + repoName
    def repoJobName =  "${folderName}/${String.format("%04d", job_name_counter++)}-plugin-pull-request-build-${repoName}"

    // job to build cosmic a plugin
    multiJob(repoJobName) {
      parameters {
        stringParam(GIT_REPO_BRANCH_PARAM, injectJobVariable(GIT_PR_BRANCH_ENV_VARIABLE_NAME), 'Branch to be built')
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
          branch(injectJobVariable(GIT_REPO_BRANCH_PARAM))
          extensions {
            cleanAfterCheckout()
            cleanBeforeCheckout()
            wipeOutWorkspace()
          }
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
              gitRevision(true)
            }
          }
        }
      }
      publishers {
        archiveJunit(makePatternList(MAVEN_REPORTS)) {
          retainLongStdout(true)
          testDataPublishers {
              publishTestStabilityData()
          }
          allowEmptyResults(repoName.matches('cosmic-(client|checkstyle)'))
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
