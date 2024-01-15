#!/bin/bash

set -e

kubectl -n platform exec $(kubectl -n platform get pod -l component=elasticsearch -o json | jq -r '.items[0].metadata.name') -- sh -c "$(cat scripts/init.sh)"
