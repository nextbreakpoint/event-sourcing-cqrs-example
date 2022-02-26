#!/bin/bash

set -e

export NEXUS_HOST=localhost
export NEXUS_PORT=8081

POSITIONAL_ARGS=()

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

declare -i n=1

delay=5
attempts=10

until bash -c "curl -v -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} \"http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d '{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}'"; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done

echo "Repository created"

exit 0