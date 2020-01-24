@Library('devops-shared-library@DevMaster') _

def PROJECT = '698.01_DevOps'
def gitRepo = 'https://kscm.kasikornbank.com:8443/698.01/jenkins-jack.git'
def gitCredentialsId = 'devops_git_user'

node('master') {
    deleteDir()
    stage('Create FreeStyle DSL Job') {
        jobDsl scriptText: """\
def PROJECT = '${PROJECT}'
job(PROJECT + '/build_jenkins_jack') {
    label('master')
    scm {
        git {
            branch('kbtg')
            remote {
                credentials('${gitCredentialsId}')
                url('${gitRepo}')
            }
        }
    }
    steps {
        dsl {
            external('pipelines/__init__/dsl.groovy')
        }
    }
}
"""
    }

    stage('Execute DSL Job') {
        build 'dsl'
    }
}