#!/bin/bash

set -e

export NEXUS_HOST=localhost
export NEXUS_PORT=8082
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=$(pipeline/get-nexus-password.sh)

declare -i n=1

delay=5
attempts=10

echo "Creating Nexus Repositories"

until bash -c "curl -v -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} \"http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d '{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}'"; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done
until bash -c "curl -v -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} \"http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/proxy\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d '{ \"name\": \"confluent-proxy\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }, \"proxy\": { \"remoteUrl\": \"https://packages.confluent.io/maven/\", \"metadataMaxAge\": \"1440\", \"contentMaxAge\": \"-1\"}, \"httpClient\": {\"autoBlock\": \"true\", \"blocked\": \"false\"}, \"negativeCache\": {\"enabled\": \"true\", \"timeToLive\": \"1440\"}}'"; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done
until bash -c "curl -v -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} \"http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/npm/proxy\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d '{ \"name\": \"npmjs-proxy\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"npm\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }, \"proxy\": { \"remoteUrl\": \"https://registry.npmjs.org\", \"metadataMaxAge\": \"1440\", \"contentMaxAge\": \"-1\"}, \"httpClient\": {\"autoBlock\": \"true\", \"blocked\": \"false\"}, \"negativeCache\": {\"enabled\": \"true\", \"timeToLive\": \"1440\"}}'"; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done

exit 0