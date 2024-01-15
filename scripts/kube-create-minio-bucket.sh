#!/bin/bash

set -e

kubectl -n platform delete job -l component=minio-init
kubectl -n platform apply -f scripts/minio-init.yaml
