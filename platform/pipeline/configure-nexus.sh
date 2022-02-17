#!/bin/bash

set -e

pipeline/check-nexus.sh

if [ $? -eq 0 ]

then

echo "Create repository"

export NEXUS_HOST=localhost
export NEXUS_PORT=38081
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=$(pipeline/get-nexus-password.sh)

curl -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X POST "http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted" -H "accept: application/json" -H "Content-Type: application/json" -d "{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}"

else

docker logs $(docker container ls -f name=pipeline-nexus-1 -q)

fi