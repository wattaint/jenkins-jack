@Library('devops-shared-library@DevMaster') _

def BaseImage = 'devops-69801-docker.artifactory.kasikornbank.com:8443/jenkins-jack-base'
def GitCommit
def GitRepoBranch = 'build-release'

def kscmClient = kscmFile.getClient(auth: [credentialsId: 'devops_git_user'])
def jfrogClient = jfrogArtifact.getClient(auth: [credentialsId: 'devopscicd101user'])

kslave(containers: [
    [name: 'kaniko', tag: 'debug-v0.16.0'],
    [name: 'oc-cli', tag: '3.11.0'],
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

        stage('Replace Build Text') {
            writeFile file: 'package.json-ori', text: readFile('package.json')
            def files = ['README.md', 'package.json']
            files.each { file ->
                writeFile file: file, text: readFile(file).replaceAll('(--build--)', "(build: ${GitCommit})")
            }
        }

        stage('Build BaseImage') {
            container('kaniko') {
                def npmrc = jfrog.getNpmrc()
                writeFile file: '.npmrc', text: npmrc.text
                writeFile file: 'Dockerfile', text: '''
                    FROM devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-buster-slim AS builder
                    ENV PATH $PATH:/app/node_modules/.bin

                    COPY .npmrc /root/.npmrc
                    RUN npm install --verbose -g vsce

                    WORKDIR /app
                    COPY package.json-ori /app/package.json
                    COPY package-lock.json /app/package-lock.json

                    ARG HTTP_PROXY

                    ENV HTTP_PROXY=${HTTP_PROXY}
                    ENV HTTP_PROXYS=${HTTP_PROXY}

                    RUN npm --verbose install

                    COPY README.md /app/README.md
                    COPY package.json /app/package.json
                    COPY .vscodeignore tsconfig.json tslint.json /app/

                    COPY images /app/images
                    COPY syntaxes /app/syntaxes
                    COPY src /app/src

                    RUN npm run compile
                    RUN vsce package

                    ARG GIT_COMMIT
                    RUN cp jenkins-jack-1.0.1.vsix /app/jenkins-jack-1.0.1--${GIT_COMMIT}.vsix

                    FROM devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-alpine3.11
                    ARG GIT_COMMIT
                    COPY --from=builder /app/jenkins-jack-1.0.1--${GIT_COMMIT}.vsix /jenkins-jack-1.0.1--${GIT_COMMIT}.vsix

                '''.stripIndent()

                kanikoBuild([
                    auth: [
                        [registry: 'devops-69801-docker.artifactory.kasikornbank.com:8443', credentialsId: 'devops_git_user']
                    ],
                    params: [
                        buildArgs: [
                            "GIT_COMMIT=${GitCommit}",
                            "HTTP_PROXY=http://172.31.97.120:8080",
                        ],
                        cacheRepo: 'localhost:5000/cache',
                        destination: BaseImage,
                        context: "dir://${WORKSPACE}",
                        dockerfile: 'Dockerfile'
                    ],
                    cacheImages: [
                        'devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-buster-slim',
                        'devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-alpine3.11'
                    ]
                ])
            }
        }

        def copyParam = [
            auth: [credentialsId: 'devops_git_user'],
            image: BaseImage,
            from: "/jenkins-jack-1.0.1--${GitCommit}.vsix",
            to: "${WORKSPACE}/jenkins-jack-1.0.1--${GitCommit}.vsix",
        ]
        stage('Copy Package') {
            container('oc-cli') {
                ocpCopy copyParam
                sh 'ls -lart'
            }
        }

        def targetGenericRepo = 'devops_cicd101'
        def itemBase = 'vscode/plugins/jenkins-jack/builds'
        stage('Push Package') {
            jfrogClient.upload([
                file: copyParam.to,
                repo: [
                    repoKey: targetGenericRepo,
                    itemPath: "${itemBase}/jenkins-jack-1.0.1--${GitCommit}.vsix",
                ],
            ])

            def buildFile = '__currrent_build.txt'
            writeFile file: buildFile, text: GitCommit
            jfrogClient.upload([
                file: "${WORKSPACE}/${buildFile}",
                repo: [
                    repoKey: targetGenericRepo,
                    itemPath: "${itemBase}/${buildFile}",
                ],
            ])
        }
    }
}

/*
def jfrogClient = jfrogArtifact.getClient(auth: [credentialsId: 'devops_git_user'])

kslave(containers: [
    [name: 'builder', image: BaseImage, auth: [credentialsId: 'devops_git_user']],
]) {
    node(POD_LABEL) {
        deleteDir()
        stage('Upload Package') {
            container('builder') {
                dir('app') {
                    sh """
                        pwd
                        ls -lart /

                        mv /jenkins-jack-1.0.1--${GitCommit}.vsix ${WORKSPACE}/jenkins-jack-1.0.1--${GitCommit}.vsix
                    """
                }

                jfrogClient.upload([
                    file: "${WORKSPACE}/jenkins-jack-1.0.1--${GitCommit}.vsix",
                    repo: [
                        repoKey: 'devops_cicd101',
                        itemPath: "vscode/plugins/jenkins-jack/builds/jenkins-jack-1.0.1--${GitCommit}.vsix",
                    ],
                ])
                jfrogClient.upload([
                    file: "${WORKSPACE}/__currrent_build.txt",
                    repo: [
                        repoKey: 'devops_cicd101',
                        itemPath: "vscode/plugin/jenkins-jack/builds/__currrent_build.txt",
                    ],
                ])

            }
        }
    }
}
*/