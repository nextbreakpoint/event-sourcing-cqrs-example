#!/bin/bash

set -e

CASSANDRA_VERSION=3.11.16
ELASTICSEARCH_VERSION=8.11.3
CP_KAFKA_VERSION=7.5.3
ZOOKEEPER_VERSION=3.9.1
PLATFORM_VERSION=1

echo "load image: nextbreakpoint/cassandra:${CASSANDRA_VERSION}-${PLATFORM_VERSION}"
minikube image load  nextbreakpoint/cassandra:${CASSANDRA_VERSION}-${PLATFORM_VERSION}

echo "load image: nextbreakpoint/elasticsearch:${ELASTICSEARCH_VERSION}-${PLATFORM_VERSION}"
minikube image load  nextbreakpoint/elasticsearch:${ELASTICSEARCH_VERSION}-${PLATFORM_VERSION}

echo "load image: nextbreakpoint/kafka:${CP_KAFKA_VERSION}-${PLATFORM_VERSION}"
minikube image load  nextbreakpoint/kafka:${CP_KAFKA_VERSION}-${PLATFORM_VERSION}

echo "load image: nextbreakpoint/schema-registry:${CP_KAFKA_VERSION}-${PLATFORM_VERSION}"
minikube image load  nextbreakpoint/schema-registry:${CP_KAFKA_VERSION}-${PLATFORM_VERSION}

echo "load image: nextbreakpoint/zookeeper:${ZOOKEEPER_VERSION}-${PLATFORM_VERSION}"
minikube image load  nextbreakpoint/zookeeper:${ZOOKEEPER_VERSION}-${PLATFORM_VERSION}
