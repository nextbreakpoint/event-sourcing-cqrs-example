#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
docker build -t ${REPOSITORY}/designs-aggregate-fetcher:${SERVICE_VERSION} .
