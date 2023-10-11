#!/bin/bash

set +e

helm uninstall integration-elasticsearch -n platform
helm uninstall integration-cassandra -n platform
helm uninstall integration-kafka -n platform
helm uninstall integration-zookeeper -n platform
helm uninstall integration-mysql -n platform
#helm uninstall integration-consul -n platform
helm uninstall integration-minio -n platform
helm uninstall integration-nginx -n platform
