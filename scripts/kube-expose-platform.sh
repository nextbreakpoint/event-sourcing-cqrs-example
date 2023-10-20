#!/bin/bash

set +e

#kubectl -n platform expose service/consul --name consul-external --port 8500 --target-port 8500 --type LoadBalancer --external-ip $(minikube ip)
kubectl -n platform expose service/minio --name minio-external --port 9001 --target-port 9001 --type LoadBalancer --external-ip $(minikube ip)
#kubectl -n platform expose service/nginx --name nginx-external --port 443 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)
