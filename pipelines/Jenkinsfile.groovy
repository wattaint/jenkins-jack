@Library('devops-shared-library@DevMaster') _

def BaseImage = 'devops-69801-docker.artifactory.kasikornbank.com:8443/jenkins-jack-base'
def GitCommit
def GitRepoBranch = 'build-release'

def kscmClient = kscmFile.getClient(auth: [credentialsId: 'devops_git_user'])

kslave(containers: [
    [name: 'kaniko', tag: 'debug-v0.16.0'],
    [name: 'registry'],
]) {
    node(POD_LABEL) {
        stage('Pull Code') {
            git branch: GitRepoBranch,
                url: 'https://kscm.kasikornbank.com:8443/698.01/jenkins-jack.git',
                credentialsId: 'devops_git_user'
            GitCommit = getCommitHash()
            BaseImage += ":${GitCommit}"
            currentBuild.displayName = "#${env.BUILD_NUMBER}_${GitCommit}"
        }

        stage('Build BaseImage') {
            container('kaniko') {
                def npmrc = jfrog.getNpmrc()
                writeFile file: '.npmrc', text: npmrc.text

                kanikoBuild([
                    auth: [
                        [registry: 'devops-69801-docker.artifactory.kasikornbank.com:8443', credentialsId: 'devops_git_user']
                    ],
                    params: [
                        buildArgs: ["GIT_COMMIT=${GitCommit}"],
                        cacheRepo: 'localhost:5000/cache',
                        destination: BaseImage,
                        context: "dir://${WORKSPACE}",
                        dockerfile: 'Dockerfile'
                    ],
                    cacheImages: ['devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-buster-slim']
                ])
            }
        }
    }
}

// def jfrogClient = jfrogArtifact.getClient(auth: [credentialsId: 'devops_git_user'])
// kslave(containers: [
//     [name: 'builder', image: BaseImage, auth: [credentialsId: 'devops_git_user']],
// ]) {
//     node(POD_LABEL) {
//         deleteDir()
//         stage('Pull Code') {
//             dir('app') {
//                 git branch: GitRepoBranch,
//                     url: 'https://kscm.kasikornbank.com:8443/698.01/jenkins-jack.git',
//                     credentialsId: 'devops_git_user'
//                 GitCommit = getCommitHash()
//             }
//             writeFile file: '__currrent_build.txt', text: GitCommit
//         }

//         stage('Build Package') {
//             container('builder') {
//                 dir('app') {
//                     sh """
//                         pwd
//                         ls -lart
//                         vsce package
//                         mv jenkins-jack-1.0.1.vsix jenkins-jack-1.0.1--${GitCommit}.vsix
//                     """
//                 }

//                 jfrogClient.upload([
//                     file: "${WORKSPACE}/jenkins-jack-1.0.1--${GitCommit}.vsix",
//                     repo: [
//                         repoKey: 'devops_cicd101',
//                         itemPath: "vscode/plugins/jenkins-jack/builds/jenkins-jack-1.0.1--${GitCommit}.vsix",
//                     ],
//                 ])
//                 jfrogClient.upload([
//                     file: "${WORKSPACE}/__currrent_build.txt",
//                     repo: [
//                         repoKey: 'devops_cicd101',
//                         itemPath: "vscode/plugin/jenkins-jack/builds/__currrent_build.txt",
//                     ],
//                 ])

//             }
//         }
//     }
//}