#!/bin/bash

set -e

NEXUS_HOST=localhost
NEXUS_PORT="8082"
NEXUS_USERNAME=admin
NEXUS_PASSWORD=password

SERVICE=""

POSITIONAL_ARGS=()

TARGET=shared

for i in "$@"; do
  case $i in
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
    --service=*)
      SERVICE="${i#*=}"
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

if [[ -z $SERVICE ]]; then
  echo "Missing or invalid value for argument: --service"
  exit 1
fi

echo "Nexus server is ${NEXUS_HOST}:${NEXUS_PORT}"

MAVEN_ARGS="-q -e -Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT}"

export NEXUS_USERNAME
export NEXUS_PASSWORD

echo "Starting service $SERVICE, hold tight..."

pushd services/$SERVICE
  mvn compile org.codehaus.mojo:exec-maven-plugin:exec@${TARGET} -s settings.xml ${MAVEN_ARGS} -Dcommon=true -Dservice=true -Dplatform=true -Dnexus=true
popd
