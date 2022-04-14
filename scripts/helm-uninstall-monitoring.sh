#!/bin/bash

set -e

helm uninstall integration-kibana -n monitoring
helm uninstall integration-elasticsearch -n monitoring
