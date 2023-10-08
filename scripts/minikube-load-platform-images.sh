#!/bin/bash

set -e

CASSANDRA_VERSION=3.11.10
ELASTICSEARCH_VERSION=7.17.1
CP_KAFKA_VERSION=6.1.0
ZOOKEEPER_VERSION=3.6.2
PLATFORM_VERSION=1

minikube image load  nextbreakpoint/cassandra:${CASSANDRA_VERSION}-${PLATFORM_VERSION}
minikube image load  nextbreakpoint/elasticsearch:${ELASTICSEARCH_VERSION}-${PLATFORM_VERSION}
minikube image load  nextbreakpoint/kafka:${CP_KAFKA_VERSION}-${PLATFORM_VERSION}
minikube image load  nextbreakpoint/zookeeper:${ZOOKEEPER_VERSION}-${PLATFORM_VERSION}
