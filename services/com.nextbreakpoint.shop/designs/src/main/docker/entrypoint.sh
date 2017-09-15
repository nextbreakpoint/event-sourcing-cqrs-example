#!/bin/bash

if [ -n "$SECRETS_BUCKET_NAME" ]; then
  aws s3 cp s3://${SECRETS_BUCKET_NAME}/environments/${ENVIRONMENT}/config/${CONFIG_NAME} /config/${CONFIG_NAME}
fi

java -Xmx1024M -jar /maven/${SERVICE_JAR} /config/${CONFIG_NAME}
