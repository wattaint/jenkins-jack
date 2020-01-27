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

ARG PACKAGE_NAME
ARG PACKAGE_VERSION

FROM node:12.14.1-buster-slim

ARG GIT_COMMIT
ARG PACKAGE_NAME
ARG PACKAGE_VERSION

COPY --from=builder /app/${PACKAGE_NAME}-${PACKAGE_VERSION}.vsix /${PACKAGE_NAME}-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix
