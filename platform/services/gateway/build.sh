#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
SERVICE_NAME=${3:-$(basename $(pwd))}
NEXUS_HOST=${4:-"host.docker.internal"}
NEXUS_PORT=${5:-"38081"}
docker build -t ${REPOSITORY}/${SERVICE_NAME}:${SERVICE_VERSION} --build-arg nexus_host=${NEXUS_HOST} --build-arg nexus_port=${NEXUS_PORT} .
