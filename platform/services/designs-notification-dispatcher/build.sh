#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
NEXUS_HOST=${3:-"localhost"}
NEXUS_PORT=${4:-"38081"}
SERVICE_NAME=${5:-$(basename $(pwd))}
docker build --progress=plain -t ${REPOSITORY}/${SERVICE_NAME}:${SERVICE_VERSION} --build-arg nexus_host=${NEXUS_HOST} --build-arg nexus_port=${NEXUS_PORT} .
