#!/bin/sh

set -e

export REPOSITORY="integration"
export VERSION="1.0.0-wip2-9091c58-1644529275" #"1.0.0-$(git rev-parse --abbrev-ref HEAD)-$(git rev-parse --short HEAD)-$(date +%s)"
export BUILD="false"
export TEST="true"

export PACTBROKER_HOST=localhost
export PACTBROKER_PORT="9292"

export NEXUS_HOST=localhost
export NEXUS_PORT="38081"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

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

export MAVEN_ARGS="-Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
export BUILD_ARGS="-Dnexus.host=host.docker.internal -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=host.docker.internal -Dpactbroker.port=${PACTBROKER_PORT}"

mvn versions:set versions:commit -DnewVersion=$VERSION -Dcommon=true -Dservices=true -Dplatform=true

#mvn clean package -DskipTests=true -s settings.xml -Dcommon=true -Dservices=true -Dnexus=true ${MAVEN_ARGS}

if [ "$BUILD" == "true" ]; then

mvn clean deploy -s settings.xml -Dcommon=true -Dservices=true -Dnexus=true ${MAVEN_ARGS}

for service in ${services[@]}; do
  pushd services/$service
   docker build --progress=plain -t ${REPOSITORY}/$service:${VERSION} --build-arg maven_args="${BUILD_ARGS}" .
  popd
done

fi

export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/sun.net.spi=ALL-UNNAMED"

if [ "$TEST" == "true" ]; then

#for service in ${services[@]}; do
#  pushd services/$service
#   JAEGER_SERVICE_NAME=$service mvn clean verify -Dgroups=integration
#  popd
#done

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -Dgroups=pact
  popd
done

for service in ${services[@]}; do
  pushd services/$service
   JAEGER_SERVICE_NAME=$service mvn clean verify -Dgroups=pact-verify
  popd
done

fi
