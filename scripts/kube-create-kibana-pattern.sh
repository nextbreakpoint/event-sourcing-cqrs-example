#!/bin/bash

set -e

curl "http://$(minikube ip):5601/api/index_patterns/index_pattern" -H "kbn-xsrf: reporting" -H "Content-Type: application/json" -d @$(pwd)/scripts/index-pattern.json

