#!/bin/sh

export HOSTED_ZONE_NAME=$(cat $ROOT/config/main.json | jq -r ".hosted_zone_name")
export ENVIRONMENT=$(cat $ROOT/config/main.json | jq -r ".environment")
export COLOUR=$(cat $ROOT/config/main.json | jq -r ".colour")

export CASSANDRA_USERNAME=$(cat $ROOT/config/main.json | jq -r ".cassandra_username")
export CASSANDRA_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".cassandra_password")

export ENVIRONMENT_SECRETS_PATH=$ROOT/secrets/environments/${ENVIRONMENT}/${COLOUR}

export CASSANDRA_HOST=${ENVIRONMENT}-${COLOUR}-swarm-worker.${HOSTED_ZONE_NAME}:9042

docker run --rm -it -v $ROOT/cassandra/scripts:/scripts cassandra:3.11 cqlsh -h $CASSANDRA_HOST -u $CASSANDRA_USERNAME -p $CASSANDRA_PASSWORD -f /scripts/$1.cql
