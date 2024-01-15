#!/bin/bash

set -e

minikube addons enable metrics-server
minikube addons enable dashboard
minikube addons enable ingress
