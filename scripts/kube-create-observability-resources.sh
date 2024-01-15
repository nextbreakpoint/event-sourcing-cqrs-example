#!/bin/bash

set -e

kubectl apply -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.52.0/jaeger-operator.yaml -n observability
kubectl apply -f scripts/jaeger.yaml

kubectl apply -f scripts/services-observability.yaml

kubectl apply -f scripts/grafana-datasource.yaml
kubectl apply -f scripts/grafana-dashboards.yaml
