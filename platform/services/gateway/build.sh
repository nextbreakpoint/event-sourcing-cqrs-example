#!/bin/sh
REPOSITORY=${1:-"nextbreakpoint"}
SERVICE_VERSION=${2:-"1.0.0"}
docker build -t ${REPOSITORY}/gateway:${SERVICE_VERSION} --build-arg github_username=${GITHUB_USERNAME} --build-arg github_password=${GITHUB_PASSWORD} .
