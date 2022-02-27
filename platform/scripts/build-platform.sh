#!/bin/bash

set -e

export REPOSITORY="integration"
export VERSION="1.0.0-$(git rev-parse --abbrev-ref HEAD)-$(git rev-parse --short HEAD)-$(date +%s)"
export DEPLOY="true"
export BUILD="true"
export PACT_TESTS="true"
export PACT_VERIFY="true"
export INTEGRATION_TESTS="true"

export TEST_DOCKER_HOST=host.docker.internal

export PACTBROKER_HOST=localhost
export PACTBROKER_PORT="9292"

export NEXUS_HOST=localhost
export NEXUS_PORT="8081"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

POSITIONAL_ARGS=()

for i in "$@"; do
  case $i in
    --version=*)
      VERSION="${i#*=}"
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
    --skip-deploy)
      DEPLOY="false"
      shift
      ;;
    --skip-build)
      BUILD="false"
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

echo "Images version is ${REPOSITORY}:${VERSION}"

if [[ $DEPLOY == "false" ]]; then
  echo "Skipping deploy"
fi

if [[ $BUILD == "false" ]]; then
  echo "Skipping build"
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
  designs-notify
  designs-render
  accounts
  authentication
  gateway
  frontend
)

export MAVEN_ARGS="-q -e -Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
export BUILD_ARGS="-q -e -Dnexus.host=${TEST_DOCKER_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${TEST_DOCKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"

mvn versions:set versions:commit -q -e -DnewVersion=$VERSION -Dcommon=true -Dservices=true -Dplatform=true

if [ "$DEPLOY" == "true" ]; then
  mvn clean deploy -q -e -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dnexus=true
fi

if [ "$BUILD" == "true" ]; then

#mvn package -q -e -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dnexus=true -DskipTests=true

for service in ${services[@]}; do
  pushd services/$service
   docker build --progress=plain -t ${REPOSITORY}/$service:${VERSION} --build-arg maven_args="${BUILD_ARGS}" .
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
