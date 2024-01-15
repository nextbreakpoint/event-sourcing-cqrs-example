#!/bin/bash

set -e

helm uninstall kube-prometheus-stack -n observability
helm uninstall fluent-bit -n observability
