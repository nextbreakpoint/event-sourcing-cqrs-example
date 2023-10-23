#!/bin/bash

set -e

REPOSITORY="integration"
VERSION="1.0.4-$(git rev-parse --abbrev-ref HEAD)-$(git rev-parse --short HEAD)-$(date +%s)"

TEST_DOCKER_HOST=host.docker.internal

PACTBROKER_HOST=localhost
PACTBROKER_PORT="9292"

NEXUS_HOST=localhost
NEXUS_PORT="8082"
NEXUS_USERNAME=admin
NEXUS_PASSWORD=password

CLEAN="true"
PACKAGE="true"
DEPLOY="true"
IMAGES="true"
UNIT_TESTS="true"
PACT_TESTS="true"
PACT_VERIFY="true"
INTEGRATION_TESTS="true"
USE_PLATFORM="false"
QUIET="false"
BUILD_SERVICES=""

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
      UNIT_TESTS="false"
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
    --services=*)
      BUILD_SERVICES="${i#*=}"
      shift
      ;;
    --quiet)
      QUIET="true"
      shift
      ;;
    --use-platform)
      USE_PLATFORM="true"
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

if [[ -z $VERSION ]]; then
  echo "Missing or invalid value for argument: --version"
  exit 1
fi

if [[ -z $REPOSITORY ]]; then
  echo "Missing or invalid value for argument: --docker-repository"
  exit 1
fi

if [[ -z $TEST_DOCKER_HOST ]]; then
  echo "Missing or invalid value for argument: --docker-host"
  exit 1
fi

if [[ -z $PACTBROKER_HOST ]]; then
  echo "Missing or invalid value for argument: --pactbroker-host"
  exit 1
fi

if [[ -z $PACTBROKER_PORT ]]; then
  echo "Missing or invalid value for argument: --pactbroker-port"
  exit 1
fi

if [[ -z $NEXUS_HOST ]]; then
  echo "Missing or invalid value for argument: --nexus-host"
  exit 1
fi

if [[ -z $NEXUS_PORT ]]; then
  echo "Missing or invalid value for argument: --nexus-port"
  exit 1
fi

if [[ -z $NEXUS_USERNAME ]]; then
  echo "Missing or invalid value for argument: --nexus-username"
  exit 1
fi

if [[ -z $NEXUS_PASSWORD ]]; then
  echo "Missing or invalid value for argument: --nexus-password"
  exit 1
fi

echo "Nexus server: ${NEXUS_HOST}:${NEXUS_PORT}"

echo "Pact server: ${PACTBROKER_HOST}:${PACTBROKER_PORT}"

echo "Docker host: ${TEST_DOCKER_HOST}"

echo "Version: ${VERSION}"

echo "Tag: ${REPOSITORY}:${VERSION}"

echo "Use platform: ${USE_PLATFORM}"

echo "Quiet: ${QUIET}"

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

if [[ ! -z $BUILD_SERVICES ]]; then
  services=(
    $BUILD_SERVICES
  )
fi

echo -n "Building services:"
for service in ${services[@]}; do
  echo -n " "$service
done
echo ""

if [[ $NEXUS_HOST == "localhost" ]]; then
  DOCKER_NEXUS_HOST=$TEST_DOCKER_HOST
else
  DOCKER_NEXUS_HOST=$NEXUS_HOST
fi

if [[ $PACTBROKER_HOST == "localhost" ]]; then
  DOCKER_PACTBROKER_HOST=$TEST_DOCKER_HOST
else
  DOCKER_PACTBROKER_HOST=$PACTBROKER_HOST
fi

if [[ $QUIET == "true" ]]; then
  VERBOSITY_ARGS="-q"
fi

MAVEN_ARGS="${VERBOSITY_ARGS} -e -Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
DOCKER_MAVEN_ARGS="${VERBOSITY_ARGS} -e -Dnexus.host=${DOCKER_NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${DOCKER_PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"

if [ "$UNIT_TESTS" == "false" ]; then
  MAVEN_ARGS="$MAVEN_ARGS -DskipTests"
  DOCKER_MAVEN_ARGS="$DOCKER_MAVEN_ARGS -DskipTests"
fi

export NEXUS_USERNAME
export NEXUS_PASSWORD

if [ "$CLEAN" == "true" ]; then
  mvn clean ${MAVEN_ARGS} -e -Dcommon=true -Dservices=true -Dplatform=true -Dnexus=true
fi

mvn versions:set versions:commit ${MAVEN_ARGS} -e -DnewVersion=$VERSION -Dcommon=true -Dservices=true -Dplatform=true

if [ "$PACKAGE" == "true" ]; then
  mvn package -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dplatform=true -Dnexus=true -DskipTests=true
fi

if [ "$DEPLOY" == "true" ]; then
  mvn deploy -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservices=true -Dnexus=true
fi

if [ "$IMAGES" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   docker build --progress=plain -t ${REPOSITORY}/$service:${VERSION} --build-arg maven_args="${DOCKER_MAVEN_ARGS}" .
  popd
done

fi

MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/sun.net.spi=ALL-UNNAMED"

if [ "$INTEGRATION_TESTS" == "true" ]; then

export USE_CONTAINERS="true"

if [ "$USE_PLATFORM" == "true" ]; then
  export START_PLATFORM="false"
else
  export START_PLATFORM="true"
fi

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -s settings.xml ${MAVEN_ARGS} -Dgroups=integration -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi

export pact_do_not_track=true

if [ "$PACT_TESTS" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -s settings.xml ${MAVEN_ARGS} -Dgroups=pact -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi

if [ "$PACT_VERIFY" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -s settings.xml ${MAVEN_ARGS} -Dgroups=pact-verify -Ddocker.host=${TEST_DOCKER_HOST}
  popd
done

fi
