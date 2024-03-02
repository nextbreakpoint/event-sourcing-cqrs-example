#!/bin/bash

set +e

MINIKUBE_IP=$(minikube ip)

#kubectl -n kube-system expose service/registry --name registry-external --port 5000 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)
docker run --rm -d --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:${MINIKUBE_IP}:5000"
