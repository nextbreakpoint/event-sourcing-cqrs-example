#!/bin/sh

export ACCOUNT_ID=$(cat $(pwd)/config/main.json | jq -r ".account_id")
export HOSTED_ZONE_NAME=$(cat $(pwd)/config/main.json | jq -r ".hosted_zone_name")
export ENVIRONMENT=$(cat $(pwd)/config/main.json | jq -r ".environment")
export COLOUR=$(cat $(pwd)/config/main.json | jq -r ".colour")

export ENVIRONMENT_SECRETS_PATH=$(pwd)/secrets/environments/${ENVIRONMENT}/${COLOUR}

export SWARM_RESOURCES_PATH=$(pwd)/swarm

export HOSTED_ZONE_NAME=$(cat $(pwd)/config/main.json | jq -r ".hosted_zone_name")

export KEYSTORE_PASSWORD=$(cat $(pwd)/config/main.json | jq -r ".keystore_password")

export MANAGER_A=$(host ${ENVIRONMENT}-${COLOUR}-swarm-manager-a.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export MANAGER_B=$(host ${ENVIRONMENT}-${COLOUR}-swarm-manager-b.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export MANAGER_C=$(host ${ENVIRONMENT}-${COLOUR}-swarm-manager-c.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export WORKER_A=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-int-a.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export WORKER_B=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-int-b.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export WORKER_C=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-int-c.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export EXT_WORKER_A=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-ext-a.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export EXT_WORKER_B=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-ext-b.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export EXT_WORKER_C=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-ext-c.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')

export CONSUL_VERSION=1.2.2
export CONSUL_IMAGE=consul:${CONSUL_VERSION}

export NGINX_VERSION=latest
export NGINX_IMAGE=nginx:${NGINX_VERSION}

export KAFKA_VERSION=1.1.0
export KAFKA_REVISION=1
export KAFKA_IMAGE=nextbreakpoint/kafka:${KAFKA_VERSION}-${KAFKA_REVISION}

export SHOP_DESIGNS_COMMAND_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/designs-command:1.0.0
export SHOP_DESIGNS_PROCESSOR_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/designs-processor:1.0.0
export SHOP_DESIGNS_QUERY_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/designs-query:1.0.0
export SHOP_DESIGNS_SSE_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/designs-sse:1.0.0
export SHOP_ACCOUNTS_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/accounts:1.0.0
export SHOP_AUTH_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/authentication:1.0.0
export SHOP_GATEWAY_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/gateway:1.0.0
export SHOP_WEBLET_ADMIN_IMAGE=${ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/${ENVIRONMENT}-${COLOUR}-shop/weblet-root:1.0.0

export ADVERTISE_EXT_WORKER_AGENT_1=$EXT_WORKER_A
export ADVERTISE_EXT_WORKER_AGENT_2=$EXT_WORKER_B
export ADVERTISE_EXT_WORKER_AGENT_3=$EXT_WORKER_C

export DOCKER_HOST=tcp://${ENVIRONMENT}-${COLOUR}-swarm-manager.${HOSTED_ZONE_NAME}:2376
export DOCKER_TLS=1
export DOCKER_CERT_PATH=${ENVIRONMENT_SECRETS_PATH}/swarm

./swarm/$1.sh $2 $3 $4 $5
