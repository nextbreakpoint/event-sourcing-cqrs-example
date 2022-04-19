#!/bin/bash

set +e

kubectl create ns pipeline
kubectl create ns platform
kubectl create ns services
kubectl create ns monitoring
