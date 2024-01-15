#!/bin/sh
ELASTICSEARCH_VERSION=8.11.3
PLATFORM_VERSION=1
docker build -t nextbreakpoint/elasticsearch:${ELASTICSEARCH_VERSION}-${PLATFORM_VERSION} --build-arg version=${ELASTICSEARCH_VERSION} .
