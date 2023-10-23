#!/bin/bash

set -e

docker compose -f docker-compose-platform.yaml -p platform down
docker compose -f docker-compose-pipeline.yaml -p pipeline down
