def PIPELINE_NAME = 'libvirt-java'
def EXECUTOR = 'executor'

def GITHUB_ORG_NAME = 'MissionCriticalCloud'
def GITHUB_REPO_NAME = 'libvirt-java'
def GITHUB_REPOSITORY = "${GITHUB_ORG_NAME}/${GITHUB_REPO_NAME}"

pipelineJob(PIPELINE_NAME) {
    triggers {
        githubPush()
    }
    logRotator {
        numToKeep(50)
        artifactNumToKeep(10)
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        github(GITHUB_REPOSITORY, 'https')
                    }
                    branches('master')
                    extensions {
                        wipeOutWorkspace()
                    }
                }
            }
        }
    }
}
