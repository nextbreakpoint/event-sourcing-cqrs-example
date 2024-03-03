#!/bin/bash

set -e

CONTAINER_NAME=$(kubectl -n platform get pod -l component=cassandra -o json | jq -r '.items[0].metadata.name')
kubectl -n platform exec "${CONTAINER_NAME}" -- cqlsh -u cassandra -p cassandra < scripts/init.cql
