#!/bin/sh

echo "Fetching secrets..."

SECRETS_BUCKET_NAME=$(cat $ROOT/config/main.json | jq -r ".secrets_bucket_name")
ENVIRONMENT=$(cat $ROOT/config/main.json | jq -r ".environment")
COLOUR=$(cat $ROOT/config/main.json | jq -r ".colour")

aws s3 cp s3://${SECRETS_BUCKET_NAME}/environments/${ENVIRONMENT}/${COLOUR}/consul/server_cert.pem ./secrets/environments/${ENVIRONMENT}/${COLOUR}/consul/server_cert.pem
aws s3 cp s3://${SECRETS_BUCKET_NAME}/environments/${ENVIRONMENT}/${COLOUR}/kafka/keystore-client.jks ./secrets/environments/${ENVIRONMENT}/${COLOUR}/kafka/keystore-client.jks
aws s3 cp s3://${SECRETS_BUCKET_NAME}/environments/${ENVIRONMENT}/${COLOUR}/kafka/truststore-client.jks ./secrets/environments/${ENVIRONMENT}/${COLOUR}/kafka/truststore-client.jks

echo "done."
