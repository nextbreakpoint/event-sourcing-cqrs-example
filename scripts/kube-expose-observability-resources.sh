#!/bin/bash

set -e

kubectl -n observability expose service/jaeger-query --name jaeger-query-external --port 16686 --target-port 16686 --type LoadBalancer --external-ip "${MINIKUBE_IP}"
