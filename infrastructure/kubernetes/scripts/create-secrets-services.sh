#!/bin/sh

kubectl delete secret web-service-config 
kubectl delete secret auth-service-config
kubectl delete secret designs-service-config
kubectl delete secret accounts-service-config

kubectl create secret generic web-service-config --from-file=../services/web-service/docker.json
kubectl create secret generic auth-service-config --from-file=../services/auth-service/docker.json
kubectl create secret generic designs-service-config --from-file=../services/designs-service/docker.json
kubectl create secret generic accounts-service-config --from-file=../services/accounts-service/docker.json
