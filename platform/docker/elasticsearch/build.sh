#!/bin/sh
ELASTICSEARCH_VERSION=7.16.3
PLATFORM_VERSION=1
docker build -t nextbreakpoint/elasticsearch:${ELASTICSEARCH_VERSION}-${PLATFORM_VERSION} --build-arg version=${ELASTICSEARCH_VERSION} .
