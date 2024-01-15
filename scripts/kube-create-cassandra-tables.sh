#!/bin/bash

set -e

kubectl -n platform exec $(kubectl -n platform get pod -l component=cassandra -o json | jq -r '.items[0].metadata.name') -- cqlsh -u cassandra -p cassandra < scripts/init.cql
