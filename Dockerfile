FROM node:12.14.1-buster-slim AS builder
ENV PATH $PATH:/app/node_modules/.bin

WORKDIR /app
COPY .npmrc /root/.npmrc
COPY package.json package-lock.json /app/

ARG HTTP_PROXY
ENV HTTP_PROXY=${HTTP_PROXY}
ENV HTTP_PROXYS=${HTTP_PROXY}

RUN npm install vsce@1.75.0 && npm --verbose install

COPY .vscodeignore tsconfig.json tslint.json /app/
COPY images /app/images
COPY syntaxes /app/syntaxes
COPY src /app/src
RUN npm run compile

FROM node:12.14.1-buster-slim

ARG GIT_COMMIT
ARG PACKAGE_NAME
ARG PACKAGE_VERSION

ENV PATH $PATH:/app/node_modules/.bin

COPY --from=builder /app/node_modules /app/node_modules

WORKDIR /app
COPY package.json package-lock.json /app/

COPY .vscodeignore tsconfig.json tslint.json /app/
COPY images /app/images
COPY syntaxes /app/syntaxes
COPY src /app/src


RUN npm run compile
RUN sed -i 's/--build--/'"$GIT_COMMIT"'/g' package.json
RUN sed -i 's/0.0.0/'"$PACKAGE_VERSION"'/g' package.json

COPY .timestamp /.timestamp
RUN vsce package