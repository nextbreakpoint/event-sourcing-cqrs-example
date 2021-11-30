#!/bin/sh

set -e

export REPOSITORY=${1:-integration}
export VERSION=${2:-1.0.0}
export BUILD="true"
export TEST="true"

export PACTBROKER_HOST=localhost
export PACTBROKER_PORT="9292"

export NEXUS_HOST=localhost
export NEXUS_PORT="38081"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

services=(
  gateway
  authentication
  accounts
  designs-notification-dispatcher
  designs-command-producer
  designs-aggregate-fetcher
  designs-event-consumer
  designs-tile-renderer
  frontend
)

export JAEGER_SERVICE_NAME=integration

export MAVEN_ARGS="-Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
export BUILD_ARGS="-Dnexus.host=host.docker.internal -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=host.docker.internal -Dpactbroker.port=${PACTBROKER_PORT}"

if [ "$BUILD" == "true" ]; then

#mvn clean deploy -s settings.xml -Dnexus=true ${MAVEN_ARGS}
#
#pushd common
# mvn clean deploy -s settings.xml -Dnexus=true ${MAVEN_ARGS}
#popd
#
#pushd services
# mvn clean deploy -s settings.xml -Dnexus=true ${MAVEN_ARGS}
#popd

for service in ${services[@]}; do
  pushd services/$service
   docker build --progress=plain -t ${REPOSITORY}/$service:${VERSION} --build-arg maven_args="${BUILD_ARGS}" .
  popd
done

fi

export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/sun.net.spi=ALL-UNNAMED" # --add-opens com.nextbreakpoint.blueprint.designsaggregatefetcher/com.nextbreakpoint.blueprint.designs=com.fasterxml.jackson.databind"

if [ "$TEST" == "true" ]; then

for service in ${services[@]}; do
  pushd services/$service
   mvn clean verify -Dgroups=integration
  popd
done

for service in ${services[@]}; do
  pushd services/$service
   mvn clean verify -Dgroups=pact
  popd
done

for service in ${services[@]}; do
  pushd services/$service
   mvn clean verify -Dgroups=pact-verify
  popd
done

fi
