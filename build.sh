#!/bin/bash

docker-compose stop
docker-compose rm -f
GIT_COMMIT=$(git rev-parse HEAD | cut -c1-8)
PACKAGE_VERSION=$(cat package.json | jq -r '.version')
PACKAGE_NAME=$(cat package.json | jq -r '.name')

export GIT_COMMIT
export PACKAGE_VERSION
export PACKAGE_NAME

echo "--------------------------"
echo "     Name: ${PACKAGE_NAME}"
echo "  Version: ${PACKAGE_VERSION}"
echo "GitCommit: ${GIT_COMMIT}"
echo "--------------------------"
echo ""

echo $GIT_COMMIT > $(pwd)/builds/commit.txt

docker-compose stop
docker-compose build docker
docker-compose up --no-start docker
id=$(docker-compose ps -q docker | cut -c1-12)
echo $id
docker cp $id:/jenkins-jack-x-${PACKAGE_VERSION}--${GIT_COMMIT}.vsix ./builds/
docker-compose rm -f

# # - kaniko
# docker-compose run --rm warmer
# docker-compose run --rm kaniko
# docker-compose stop
# docker-compose rm -f

#code --install-extension builds/jenkins-jack-x-1.0.1-x1--$(cat builds/commit.txt).vsix