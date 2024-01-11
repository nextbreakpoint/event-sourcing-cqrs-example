#!/bin/sh
CP_SCHEMA_REGISTRY_VERSION=7.5.3
PLATFORM_VERSION=1
docker build -t nextbreakpoint/schema-registry:${CP_SCHEMA_REGISTRY_VERSION}-${PLATFORM_VERSION} --build-arg version=${CP_SCHEMA_REGISTRY_VERSION} .
