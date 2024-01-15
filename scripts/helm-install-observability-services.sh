#!/bin/bash

set -e

helm upgrade --install integration-elasticsearch helm/elasticsearch -n observability --set dataDirectory=/volumes/observability/elasticsearch-data
helm upgrade --install integration-kibana helm/kibana -n observability --set server.publicBaseUrl=http://$(minikube ip)::5601

