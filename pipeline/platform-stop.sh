#!/bin/bash

set -e

docker compose -f docker-compose-platform.yaml -p platform down
