#!/bin/bash

set -e

helm uninstall integration-pactbroker helm/pactbroker -n pipeline
helm uninstall integration-postgres helm/postgres -n pipeline
helm uninstall integration-nexus helm/nexus -n pipeline
