#!/bin/bash

set -e

./scripts/build-services.sh --docker-host="172.17.0.1" --nexus-password=$(./pipeline/get-nexus-password.sh) --quiet --use-platform

