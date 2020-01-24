FROM devops-69801-docker.artifactory.kasikornbank.com:8443/node:12.14.1-buster-slim
ENV PATH $PATH:/app/node_modules/.bin

COPY .npmrc /root/.npmrc
RUN npm install --verbose -g vsce

WORKDIR /app
COPY package.json package-lock.json /app/

ARG HTTP_PROXY

ENV HTTP_PROXY=${HTTP_PROXY}
ENV HTTP_PROXYS=${HTTP_PROXY}

RUN npm --verbose install

COPY .vscodeignore tsconfig.json tslint.json /app/

COPY images /app/images
COPY syntaxes /app/syntaxes
COPY src /app/src

RUN npm run compile
RUN vsce package

#ARG GIT_COMMIT
#RUN mv jenkins-jack-1.0.1.vsix jenkins-jack-1.0.1--${GIT_COMMIT}.vsix
