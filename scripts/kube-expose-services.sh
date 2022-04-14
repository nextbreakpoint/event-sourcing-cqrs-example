#!/bin/bash

set -e

kubectl -n services expose service/designs-notify --name designs-notify-external --port 8000 --target-port 8080 --type LoadBalancer --external-ip $(minikube ip)
