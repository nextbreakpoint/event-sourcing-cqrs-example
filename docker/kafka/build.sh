#!/bin/sh
CP_KAFKA_VERSION=7.5.3
PLATFORM_VERSION=1
docker build -t nextbreakpoint/kafka:${CP_KAFKA_VERSION}-${PLATFORM_VERSION} --build-arg version=${CP_KAFKA_VERSION} .
