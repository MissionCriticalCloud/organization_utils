def FOLDER_NAME = 'mcc-jenkins'

def SEED_JOB = "${FOLDER_NAME}/seed-job"
def CONNECT_AGENT_JOB = "${FOLDER_NAME}/connect-agent-job"

def HOSTNAME_PARAM = 'hostname'

def ORGANIZATION_NAME = 'MissionCriticalCloud'
def ORGANIZATION_UTILS_REPOSITORY_NAME = 'organization_utils'
def ORGANIZATION_UTILS_GITHUB_REPOSITORY = "${ORGANIZATION_NAME}/${ORGANIZATION_UTILS_REPOSITORY_NAME}"
def ORGANIZATION_UTILS_GITHUB_DEFAULT_BRANCH = 'master'

def DEFAULT_EXECUTOR = 'master'

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
                    includedRegions 'jenkins/jobs/jenkins_jobs[.]groovy'
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
            external('jenkins/jobs/jenkins_jobs.groovy')
        }
    }
}

freeStyleJob(CONNECT_AGENT_JOB) {
    parameters {
        stringParam(HOSTNAME_PARAM, '', 'Hostname of node to reconnect.')
    }
    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }
    label(DEFAULT_EXECUTOR)
    steps {
        systemGroovyCommand("""
import jenkins.model.Jenkins

import java.util.concurrent.*

def hostname = build.buildVariableResolver.resolve('${HOSTNAME_PARAM}')

def node = Jenkins.getInstance().getNode(hostname)

def getEnviron(computer) {
    def env
    def thread = Thread.start("Getting env from \${computer.name}", { env = computer.environment })
    thread.join(2000)
    if (thread.isAlive()) thread.interrupt()
    env
}

def slaveAccessible(computer) {
    getEnviron(computer)?.get('PATH') != null
}

if (node != null) {
    def computer = node.computer
    println ""
    println "Checking computer \${computer.name}:"
    println ""
    def isOK = (slaveAccessible(computer) && !computer.offline)
    if (isOK) {
        println "\\t\\tOK, got PATH back from slave \${computer.name}."
        println ""
        println('\\tcomputer.isOffline: ' + computer.isOffline())
        println('\\tcomputer.isTemporarilyOffline: ' + computer.isTemporarilyOffline())
    } else {
        println "\\t\\tERROR: can't get PATH from slave \${computer.name}."
        println ""
        println('\\tcomputer.isOffline: ' + computer.isOffline())
        println('\\tcomputer.isTemporarilyOffline: ' + computer.isTemporarilyOffline())
        if (computer.isTemporarilyOffline()) {
            if (!computer.getOfflineCause().toString().contains("Disconnected by")) {
                computer.setTemporarilyOffline(false, computer.getOfflineCause())
            }
        } else {
            println("Node is offline, trying to reconnect to " + hostname)
            computer.connect(true)
        }
    }
} else {
    println("Could not find Node with hostname: " + hostname)
}

println ""
"""
        )
    }
}
