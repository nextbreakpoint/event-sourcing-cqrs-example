#!/bin/bash

set -x
set -e

docker compose -f docker-compose-pipeline.yaml -p pipeline up -d
