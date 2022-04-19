#!/bin/bash

set +e

kubectl -n monitoring expose service/kube-prometheus-stack-grafana --name grafana-external --port 3000 --target-port 3000 --type LoadBalancer --external-ip $(minikube ip)
kubectl -n monitoring expose service/prometheus-operated --name prometheus-external --port 9090 --target-port 9090 --type LoadBalancer --external-ip $(minikube ip)
kubectl -n monitoring expose service/kibana --name=kibana-external --port=5601 --target-port=5601 --type=LoadBalancer --external-ip=$(minikube ip)
kubectl -n monitoring expose service/jaeger-query --name=jaeger-query-external --port=16686 --target-port=16686 --type=LoadBalancer --external-ip=$(minikube ip)
