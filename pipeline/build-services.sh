#!/bin/bash

set -e

./scripts/build-services.sh --nexus-password=$(./pipeline/get-nexus-password.sh) --skip-pact-tests --skip-pact-verify --use-platform --quiet

