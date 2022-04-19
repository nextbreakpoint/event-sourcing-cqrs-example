#!/bin/bash

set -e

helm upgrade --install integration-elasticsearch helm/elasticsearch -n monitoring --set dataDirectory=/volumes/monitoring/elasticsearch-data
helm upgrade --install integration-kibana helm/kibana -n monitoring --set server.publicBaseUrl=http://$(minikube ip)::5601
