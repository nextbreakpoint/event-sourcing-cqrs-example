#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
NEXUS_HOST=${3:-"localhost"}
NEXUS_PORT=${4:-"8082"}
PACTBROKER_HOST=${5:-"localhost"}
PACTBROKER_PORT=${6:-"9292"}
SERVICE_NAME=$(basename $(pwd))
MAVEN_ARGS="-Dnexus.host=${NEXUS_HOST} -Dnexus.port=${NEXUS_PORT} -Dpactbroker.host=${PACTBROKER_HOST} -Dpactbroker.port=${PACTBROKER_PORT}"
docker build --progress=plain -t ${REPOSITORY}/${SERVICE_NAME}:${SERVICE_VERSION} --build-arg maven_args="${MAVEN_ARGS}" .
