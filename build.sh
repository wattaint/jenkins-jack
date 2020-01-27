#!/bin/bash

GIT_COMMIT=$(git rev-parse HEAD | cut -c1-8)
PACKAGE_VERSION=$(cat package.json | jq -r '.version')
PACKAGE_NAME=$(cat package.json | jq -r '.name')

export GIT_COMMIT
export PACKAGE_VERSION
export PACKAGE_NAME

echo "     Name: ${PACKAGE_NAME}"
echo "  Version: ${PACKAGE_VERSION}"
echo "GitCommit: ${GIT_COMMIT}"

echo $GIT_COMMIT > $(pwd)/builds/commit.txt

docker-compose stop
docker-compose build docker
docker-compose up --no-start docker
id=$(docker-compose ps -q docker | cut -c1-12)
echo $id
docker cp $id:/jenkins-jack-x-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix ./builds/
docker-compose rm -f