#!/bin/bash

set -e

export REPOSITORY="integration"
export VERSION="1.0.0-$(git rev-parse --abbrev-ref HEAD)-$(git rev-parse --short HEAD)-$(date +%s)"

export TEST_DOCKER_HOST=host.docker.internal

export PACTBROKER_HOST=localhost
export PACTBROKER_PORT="9292"

export NEXUS_HOST=localhost
export NEXUS_PORT="8082"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

CLEAN="true"
PACKAGE="true"
DEPLOY="true"
IMAGES="true"
PACT_TESTS="true"
PACT_VERIFY="true"
INTEGRATION_TESTS="true"

POSITIONAL_ARGS=()

for i in "$@"; do
  case $i in
    --version=*)
      VERSION="${i#*=}"
      shift
      ;;
    --keep-version)
      VERSION="$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)"
      DEPLOY="false"
      shift
      ;;
    --docker-repository=*)
      REPOSITORY="${i#*=}"
      shift
      ;;
    --docker-host=*)
      TEST_DOCKER_HOST="${i#*=}"
      shift
      ;;
    --pactbroker-host=*)
      PACTBROKER_HOST="${i#*=}"
      shift
      ;;
    --pactbroker-port=*)
      PACTBROKER_PORT="${i#*=}"
      shift
      ;;
    --nexus-host=*)
      NEXUS_HOST="${i#*=}"
      shift
      ;;
    --nexus-port=*)
      NEXUS_PORT="${i#*=}"
      shift
      ;;
    --nexus-username=*)
      NEXUS_USERNAME="${i#*=}"
      shift
      ;;
    --nexus-password=*)
      NEXUS_PASSWORD="${i#*=}"
      shift
      ;;
    --skip-clean)
      CLEAN="false"
      shift
      ;;
    --skip-package)
      PACKAGE="false"
      shift
      ;;
    --skip-deploy)
      DEPLOY="false"
      shift
      ;;
    --skip-images)
      IMAGES="false"
      shift
      ;;
    --skip-tests)
      PACT_TESTS="false"
      PACT_VERIFY="false"
      INTEGRATION_TESTS="false"
      shift
      ;;
    --skip-pact-tests)
      PACT_TESTS="false"
      shift
      ;;
    --skip-pact-verify)
      PACT_VERIFY="false"
      shift
      ;;
    --skip-integration-tests)
      INTEGRATION_TESTS="false"
      shift
      ;;
    -*|--*)
      echo "Unknown option $i"
      exit 1
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ -z $NEXUS_USERNAME ]]; then
  echo "Missing required parameter --nexus-username"
  exit 1
fi

if [[ -z $NEXUS_PASSWORD ]]; then
  echo "Missing required parameter --nexus-password"
  exit 1
fi

echo "Nexus server is ${NEXUS_HOST}:${NEXUS_PORT}"

echo "Pact server is ${PACTBROKER_HOST}:${PACTBROKER_PORT}"

echo "Docker host is ${TEST_DOCKER_HOST}"

echo "Version is ${VERSION}"

echo "Tag is ${REPOSITORY}:${VERSION}"

if [[ $CLEAN == "false" ]]; then
  echo "Skipping clean"
fi

if [[ $PACKAGE == "false" ]]; then
  echo "Skipping package"
fi

if [[ $DEPLOY == "false" ]]; then
  echo "Skipping deploy"
fi

if [[ $IMAGES == "false" ]]; then
  echo "Skipping images"
fi

if [[ $INTEGRATION_TESTS == "false" ]]; then
  echo "Skipping integration tests"
fi

if [[ $PACT_TESTS == "false" ]]; then
  echo "Skipping pact tests"
fi

if [[ $PACT_VERIFY == "false" ]]; then
  echo "Skipping pact verification"
fi

services=(
  designs-query
  designs-command
  designs-aggregate
  designs-watch
  designs-render
  accounts
  authentication
  frontend
)

if [[ $NEXUS_HOST == "localhost" ]]; then
  DOCKER_NEXUS_HOST="host.docker.internal"
else
  DOCKER_NEXUS_HOST=$NEXUS_HOST
fi

if [[ $PACTBROKER_HOST == "localhost" ]]; then
  DOCKER_PACTBROKER_HOST="host.docker.internal"
else
  DOCKER_PACTBROKER_HOST=$PACTBROKER_HOST
fi

MAVEN_ARGS="-q -e -Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
DOCKER_MAVEN_ARGS="-q -e -Dnexus.host=${DOCKER_NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${DOCKER_PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"

if [ "$CLEAN" == "true" ]; then
  mvn clean -q -e -Dcommon=true -Dservices=true -Dplatform=true -Dnexus=true
fi

mvn versions:set versions:commit -q -e -DnewVersion=$VERSION -Dcommon=true -Dservices=true -Dplatform=true

if [ "$PACKAGE" == "true" ]; then
  mvn package -q -e -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dplatform=true -Dnexus=true -DskipTests=true
fi

if [ "$DEPLOY" == "true" ]; then
  mvn deploy -q -e -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dnexus=true
fi

if [ "$IMAGES" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   docker build --progress=plain -t ${REPOSITORY}/$service:${VERSION} --build-arg maven_args="${DOCKER_MAVEN_ARGS}" .
  popd
done

fi

export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/sun.net.spi=ALL-UNNAMED"

if [ "$INTEGRATION_TESTS" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -q -e -Dgroups=integration -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi

export pact_do_not_track=true

if [ "$PACT_TESTS" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -q -e -Dgroups=pact -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi

if [ "$PACT_VERIFY" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -q -e -Dgroups=pact-verify -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi
