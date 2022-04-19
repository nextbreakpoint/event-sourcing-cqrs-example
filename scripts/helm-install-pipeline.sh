#!/bin/bash

set -e

helm upgrade --install integration-nexus helm/nexus -n pipeline
helm upgrade --install integration-postgres helm/postgres -n pipeline
helm upgrade --install integration-pactbroker helm/pactbroker -n pipeline
