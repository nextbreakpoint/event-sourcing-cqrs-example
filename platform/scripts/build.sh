#!/bin/sh

set -e

export REPOSITORY=${1:-integration}
export VERSION=${2:-1.0.0}
export BUILD="false"
export TEST="true"

export PACTBROKER_HOST=$(minikube ip)
export PACTBROKER_PORT="9292"

export NEXUS_HOST=$(minikube ip)
export NEXUS_PORT="8081"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

services=( \
  gateway \
  authentication \
  accounts \
  designs-notification-dispatcher \
  designs-command-producer \
  designs-aggregate-fetcher \
  designs-event-consumer \
  designs-tile-renderer \
#  frontend \
)

export JAEGER_SERVICE_NAME=integration

if [ "$BUILD" == "true" ]; then

#mvn clean deploy -s settings.xml -Dnexus=true
#
#pushd common
# mvn clean deploy -s settings.xml -Dnexus=true
#popd
#
#pushd services
# mvn clean deploy -s settings.xml -Dnexus=true
#popd

for service in ${services[@]}; do
  pushd services/$service
   sh build.sh ${REPOSITORY} ${VERSION} ${NEXUS_HOST} ${NEXUS_PORT} ${PACTBROKER_HOST} ${PACTBROKER_PORT}
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
