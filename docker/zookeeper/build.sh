#!/bin/sh
ZOOKEEPER_VERSION=3.6.2
PLATFORM_VERSION=1
docker build -t nextbreakpoint/zookeeper:${ZOOKEEPER_VERSION}-${PLATFORM_VERSION} --build-arg version=${ZOOKEEPER_VERSION} .
