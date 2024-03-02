#!/bin/bash

set +e

MINIKUBE_IP=$(minikube ip)

kubectl -n observability expose service/kube-prometheus-stack-grafana --name grafana-external --port 3000 --target-port 3000 --type LoadBalancer --external-ip "${MINIKUBE_IP}"
kubectl -n observability expose service/prometheus-operated --name prometheus-external --port 9090 --target-port 9090 --type LoadBalancer --external-ip "${MINIKUBE_IP}"
kubectl -n observability expose service/kibana --name kibana-external --port 5601 --target-port 5601 --type LoadBalancer --external-ip "${MINIKUBE_IP}"
