#!/bin/sh

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
export WORKER_A=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-a.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export WORKER_B=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-b.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')
export WORKER_C=$(host ${ENVIRONMENT}-${COLOUR}-swarm-worker-c.${HOSTED_ZONE_NAME} | grep -m1 " has address " | awk '{ print $4 }')

export DOCKER_HOST=tcp://${ENVIRONMENT}-${COLOUR}-swarm-manager.${HOSTED_ZONE_NAME}:2376
export DOCKER_TLS=1
export DOCKER_CERT_PATH=${ENVIRONMENT_SECRETS_PATH}/swarm

./swarm/$1.sh $2 $3 $4 $5
