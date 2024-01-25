#!/bin/sh
CASSANDRA_VERSION=4.1.3
PLATFORM_VERSION=1
docker build -t nextbreakpoint/cassandra:${CASSANDRA_VERSION}-${PLATFORM_VERSION} --build-arg version=${CASSANDRA_VERSION} .
