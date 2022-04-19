#!/bin/bash

set +e

kubectl -n platform delete secret nginx

kubectl -n services delete secret keystore-server.jks
kubectl -n services delete secret keystore-client.jks
kubectl -n services delete secret truststore-server.jks
kubectl -n services delete secret truststore-client.jks
kubectl -n services delete secret keystore-auth.jceks


kubectl -n services delete secret authentication

kubectl -n services delete secret accounts

kubectl -n services delete secret designs-query
kubectl -n services delete secret designs-command
kubectl -n services delete secret designs-aggregate
kubectl -n services delete secret designs-notify
kubectl -n services delete secret designs-render

kubectl -n services delete secret gateway

kubectl -n services delete secret frontend
