#!/bin/bash

env

if [ -n "$SECRETS_BUCKET_NAME" ]; then
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/config/$CONFIG_NAME /config/$CONFIG_NAME
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/keystores/keystore-auth.jceks /keystores/keystore-auth.jceks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/keystores/keystore-client.jks /keystores/keystore-client.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/keystores/keystore-server.jks /keystores/keystore-server.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/keystores/truststore-client.jks /keystores/truststore-client.jks
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/shop/keystores/truststore-server.jks /keystores/truststore-server.jks
fi

java -Xmx512M --add-modules java.xml.bind -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.net.dns=ALL-UNNAMED -jar /maven/$SERVICE_JAR /config/$CONFIG_NAME
