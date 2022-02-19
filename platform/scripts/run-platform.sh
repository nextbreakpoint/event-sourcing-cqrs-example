#!/bin/bash

set -e

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
)

export MAVEN_ARGS="-q -e -Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"

for service in ${services[@]}; do
  pushd services/$service
    mvn compile org.codehaus.mojo:exec-maven-plugin:exec@shared -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservice=true -Dplatform=true -Dnexus=true &
  popd
done
