def PROJECT = '698.01_DevOps'

pipelineJob(PROJECT + '/build_jenkins_jack') {
    definition {
        cpsScm {
            scm {
                git {
                    branch('kbtg')
                    remote {
                        url('https://kscm.kasikornbank.com:8443/698.01/jenkins-jack.git')
                        credentials('devops_git_user')
                    }
                }
            }
            scriptPath('pipelines/Jenkinsfile.groovy')
        }
    }
}