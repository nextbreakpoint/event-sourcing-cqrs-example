#!/bin/bash

set +e

helm uninstall integration-pactbroker -n pipeline
helm uninstall integration-postgres -n pipeline
helm uninstall integration-nexus -n pipeline
