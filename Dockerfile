FROM node:12.14.1-buster-slim AS builder
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

ARG GIT_COMMIT=latest
ARG PACKAGE_VERSION

RUN ls -lart
RUN cp jenkins-jack-x-${PACKAGE_VERSION}.vsix /app/jenkins-jack-x-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix

FROM node:12.14.1-buster-slim

ARG GIT_COMMIT=latest
ARG PACKAGE_VERSION

COPY --from=builder /app/jenkins-jack-x-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix /jenkins-jack-x-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix
