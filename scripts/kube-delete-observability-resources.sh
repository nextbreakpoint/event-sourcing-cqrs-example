#!/bin/bash

set -e

kubectl delete -f scripts/grafana-datasource.yaml
kubectl delete -f scripts/grafana-dashboards.yaml

kubectl delete -f scripts/services-observability.yaml

kubectl delete -f scripts/jaeger.yaml
kubectl delete -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.52.0/jaeger-operator.yaml -n observability
