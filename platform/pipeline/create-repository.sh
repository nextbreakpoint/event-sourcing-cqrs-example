#!/bin/bash

set -e

export NEXUS_HOST=localhost
export NEXUS_PORT=38081
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=$(pipeline/get-nexus-password.sh)

declare -i n=1

delay=5
attempts=10

until bash -c "curl -v -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} \"http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d '{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}'"; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done

echo "Repository created"

exit 0