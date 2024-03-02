#!/bin/bash

set -e

helm upgrade --install integration-elasticsearch helm/elasticsearch -n platform
helm upgrade --install integration-cassandra helm/cassandra -n platform
helm upgrade --install integration-zookeeper helm/zookeeper -n platform
helm upgrade --install integration-kafka helm/kafka -n platform --set "externalName=$(minikube ip):9093"
helm upgrade --install integration-schema-registry helm/schema-registry -n platform
helm upgrade --install integration-mysql helm/mysql -n platform
#helm upgrade --install integration-consul helm/consul -n platform --set "servicePort=8080,serviceName=$(minikube ip)"
helm upgrade --install integration-minio helm/minio -n platform
helm upgrade --install integration-nginx helm/nginx -n platform --set hostname=minikube,certificate="$(base64 < ./secrets/nginx_server_cert.pem)",privateKey="$(base64 < ./secrets/nginx_server_key.pem)"
