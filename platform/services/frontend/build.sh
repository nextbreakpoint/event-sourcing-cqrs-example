#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
SERVICE_NAME=${3:-$(basename $(pwd))}
docker build -t ${REPOSITORY}/${SERVICE_NAME}:${SERVICE_VERSION} .
