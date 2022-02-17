#!/bin/bash

set -e

docker compose -f docker-compose-pipeline.yaml -p pipeline down
