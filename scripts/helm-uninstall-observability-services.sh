#!/bin/bash

set +e

helm uninstall integration-kibana -n observability
helm uninstall integration-elasticsearch -n observability
