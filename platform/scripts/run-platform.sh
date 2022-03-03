#!/bin/bash

set -e

PACTBROKER_HOST=localhost
PACTBROKER_PORT="9292"

NEXUS_HOST=localhost
NEXUS_PORT="8081"
NEXUS_USERNAME=admin
NEXUS_PASSWORD=password

TARGET=shared

POSITIONAL_ARGS=()

for i in "$@"; do
  case $i in
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
    --isolated)
      TARGET="isolated"
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
    mvn compile org.codehaus.mojo:exec-maven-plugin:exec@${TARGET} -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservice=true -Dplatform=true -Dnexus=true &
  popd
done
