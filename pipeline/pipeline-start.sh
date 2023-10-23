#!/bin/bash

set -e

docker compose -f docker-compose-pipeline.yaml -p pipeline up -d
docker compose -f docker-compose-platform.yaml -p platform up -d

docker ps
