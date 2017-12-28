#!/bin/sh

kubectl create secret generic keystores --from-file=../keystores/keystore-auth.jceks --from-file=../keystores/keystore-client.jks --from-file=../keystores/keystore-server.jks --from-file=../keystores/truststore-client.jks --from-file=../keystores/truststore-server.jks
