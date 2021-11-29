#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
SERVICE_NAME=$(basename $(pwd))
docker build --progress=plain -t ${REPOSITORY}/${SERVICE_NAME}:${SERVICE_VERSION} .
