#!/bin/bash

set +e

kubectl -n pipeline expose service/nexus --name nexus-external --port 8081 --target-port 8081 --type LoadBalancer --external-ip $(minikube ip)
kubectl -n pipeline expose service/pactbroker --name pactbroker-external --port 9292 --target-port 9292 --type LoadBalancer --external-ip $(minikube ip)
