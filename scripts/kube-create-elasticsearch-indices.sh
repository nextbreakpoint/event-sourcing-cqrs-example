#!/bin/bash

set -e

CONTAINER_NAME=$(kubectl -n platform get pod -l component=elasticsearch -o json | jq -r '.items[0].metadata.name')
kubectl -n platform exec "${CONTAINER_NAME}" -- sh -c "$(cat scripts/init.sh)"
