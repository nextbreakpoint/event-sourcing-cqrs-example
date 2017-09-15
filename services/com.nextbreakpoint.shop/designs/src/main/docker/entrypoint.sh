#!/bin/bash

env

if [ -n "$SECRETS_BUCKET_NAME" ]; then
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/config/$CONFIG_NAME /config/$CONFIG_NAME
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/keystores/keystore-auth.jceks /keystores/keystore-auth.jceks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/keystores/keystore-client.jks /keystores/keystore-client.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/keystores/keystore-server.jks /keystores/keystore-server.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/keystores/truststore-client.jks /keystores/truststore-client.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/keystores/truststore-server.jks /keystores/truststore-server.jks
fi

java -Xmx1024M -jar /maven/$SERVICE_JAR /config/$CONFIG_NAME
