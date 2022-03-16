#!/bin/sh

# if [ -z "$KEYSTORE_LOCATION" ]; then
#   if [ -n "$KEYSTORE_CONTENT" ]; then
#     echo $KEYSTORE_CONTENT | base64 -d > /var/tmp/keystore.jks
#     KEYSTORE_LOCATION=/var/tmp/keystore.jks
#   fi
# fi
#
# if [ -z "$TRUSTSTORE_LOCATION" ]; then
#   if [ -n "$TRUSTSTORE_CONTENT" ]; then
#     echo $TRUSTSTORE_CONTENT | base64 -d > /var/tmp/truststore.jks
#     TRUSTSTORE_LOCATION=/var/tmp/truststore.jks
#   fi
# fi

# export KAFKA_OPTS="-javaagent:/opt/jmx-exporter/jmx-exporter.jar=7070:/etc/jmx-exporter/config.yml"

# if [ -z "$JAAS_CONFIG_LOCATION" ]; then
#   if [ -n "$JAAS_CONFIG_CONTENT" ]; then
#     echo $JAAS_CONFIG_CONTENT | base64 -d > /var/tmp/kafka_jaas.conf
#     JAAS_CONFIG_LOCATION="/var/tmp/kafka_jaas.conf"
#   fi
# fi

# if [ -n "$JAAS_CONFIG_LOCATION" ]; then
#   export KAFKA_OPTS="$KAFKA_OPTS -Djava.security.auth.login.config=$JAAS_CONFIG_LOCATION"
# fi
#
# if [ -n "$KEYSTORE_LOCATION" ]; then
#   if [ -n "$KEYSTORE_PASSWORD" ]; then
#     if [ -n "$TRUSTSTORE_LOCATION" ]; then
#       if [ -n "$TRUSTSTORE_PASSWORD" ]; then
# cat <<EOF > /var/tmp/client-ssl.properties
# security.protocol=SSL
# ssl.truststore.location=${TRUSTSTORE_LOCATION}
# ssl.truststore.password=${TRUSTSTORE_PASSWORD}
# ssl.keystore.location=${KEYSTORE_LOCATION}
# ssl.keystore.password=${KEYSTORE_PASSWORD}
# EOF
#       fi
#     fi
#   fi
# fi

unset KAFKA_HOST
unset KAFKA_PORT
exec "$@"
