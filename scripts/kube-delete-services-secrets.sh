#!/bin/bash

set +e

kubectl -n services delete secret keystore-auth.jceks

kubectl -n services delete secret authentication
kubectl -n services delete secret accounts
kubectl -n services delete secret designs-query
kubectl -n services delete secret designs-command
kubectl -n services delete secret designs-aggregate
kubectl -n services delete secret designs-watch
kubectl -n services delete secret designs-render

