#!/bin/sh

HOSTED_ZONE_NAME=$(cat $ROOT/config/main.json | jq -r ".hosted_zone_name")
ENVIRONMENT=$(cat $ROOT/config/main.json | jq -r ".environment")
COLOUR=$(cat $ROOT/config/main.json | jq -r ".colour")

KEY_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".keystore_password")
KEYSTORE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".keystore_password")
TRUSTSTORE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".truststore_password")

OUTPUT_GEN=$ROOT/secrets/generated/$ENVIRONMENT/$COLOUR
OUTPUT_ENV=$ROOT/secrets/environments/$ENVIRONMENT/$COLOUR

echo "Generating secrets for environment ${ENVIRONMENT} of colour ${COLOUR} into directory ${OUTPUT_ENV}"

if [ ! -d "$OUTPUT_GEN" ]; then

mkdir -p $OUTPUT_GEN

echo '[extended]\nextendedKeyUsage=serverAuth,clientAuth\nkeyUsage=digitalSignature,keyAgreement' > $OUTPUT_GEN/openssl.cnf

## Create keystore for JWT authentication
keytool -genseckey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass $KEY_PASSWORD
keytool -genseckey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass $KEY_PASSWORD
keytool -genseckey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass $KEY_PASSWORD
keytool -genkey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg RSA -keysize 2048 -alias RS256 -keypass $KEY_PASSWORD -sigalg SHA256withRSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365
keytool -genkey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg RSA -keysize 2048 -alias RS384 -keypass $KEY_PASSWORD -sigalg SHA384withRSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365
keytool -genkey -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg RSA -keysize 2048 -alias RS512 -keypass $KEY_PASSWORD -sigalg SHA512withRSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365
keytool -genkeypair -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg EC -keysize 256 -alias ES256 -keypass $KEY_PASSWORD -sigalg SHA256withECDSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365
keytool -genkeypair -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg EC -keysize 256 -alias ES384 -keypass $KEY_PASSWORD -sigalg SHA384withECDSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365
keytool -genkeypair -keystore $OUTPUT_GEN/keystore-auth.jceks -storetype JCEKS -storepass $KEYSTORE_PASSWORD -keyalg EC -keysize 256 -alias ES512 -keypass $KEY_PASSWORD -sigalg SHA512withECDSA -dname "CN=${HOSTED_ZONE_NAME}" -validity 365

## Create certificate authority (CA)
openssl req -new -x509 -keyout $OUTPUT_GEN/ca_key.pem -out $OUTPUT_GEN/ca_cert.pem -days 365 -passin pass:$KEY_PASSWORD -passout pass:$KEY_PASSWORD -subj "/CN=${HOSTED_ZONE_NAME}"

## Create client keystore
keytool -noprompt -keystore $OUTPUT_GEN/keystore-client.jks -genkey -alias selfsigned -dname "CN=${HOSTED_ZONE_NAME}" -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass $KEYSTORE_PASSWORD -keypass $KEY_PASSWORD
openssl pkcs12 -in $OUTPUT_GEN/keystore-client.jks -nocerts -nodes -passin pass:$KEYSTORE_PASSWORD -out $OUTPUT_GEN/client_key.pem

## Create server keystore
keytool -noprompt -keystore $OUTPUT_GEN/keystore-server.jks -genkey -alias selfsigned -dname "CN=${HOSTED_ZONE_NAME}" -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass $KEYSTORE_PASSWORD -keypass $KEY_PASSWORD
openssl pkcs12 -in $OUTPUT_GEN/keystore-server.jks -nocerts -nodes -passin pass:$KEYSTORE_PASSWORD -out $OUTPUT_GEN/server_key.pem

## Sign client certificate
keytool -noprompt -keystore $OUTPUT_GEN/keystore-client.jks -alias selfsigned -certreq -file $OUTPUT_GEN/client_csr.pem -storepass $KEYSTORE_PASSWORD
openssl x509 -extfile $OUTPUT_GEN/openssl.cnf -extensions extended -req -CA $OUTPUT_GEN/ca_cert.pem -CAkey $OUTPUT_GEN/ca_key.pem -in $OUTPUT_GEN/client_csr.pem -out $OUTPUT_GEN/client_cert.pem -days 365 -CAcreateserial -passin pass:$KEY_PASSWORD

## Sign server certificate
keytool -noprompt -keystore $OUTPUT_GEN/keystore-server.jks -alias selfsigned -certreq -file $OUTPUT_GEN/server_csr.pem -storepass $KEYSTORE_PASSWORD
openssl x509 -extfile $OUTPUT_GEN/openssl.cnf -extensions extended -req -CA $OUTPUT_GEN/ca_cert.pem -CAkey $OUTPUT_GEN/ca_key.pem -in $OUTPUT_GEN/server_csr.pem -out $OUTPUT_GEN/server_cert.pem -days 365 -CAcreateserial -passin pass:$KEY_PASSWORD

## Import CA and client signed certificate into client keystore
keytool -noprompt -keystore $OUTPUT_GEN/keystore-client.jks -alias CARoot -import -file $OUTPUT_GEN/ca_cert.pem -storepass $KEYSTORE_PASSWORD
keytool -noprompt -keystore $OUTPUT_GEN/keystore-client.jks -alias selfsigned -import -file $OUTPUT_GEN/client_cert.pem -storepass $KEYSTORE_PASSWORD

## Import CA and server signed certificate into server keystore
keytool -noprompt -keystore $OUTPUT_GEN/keystore-server.jks -alias CARoot -import -file $OUTPUT_GEN/ca_cert.pem -storepass $KEYSTORE_PASSWORD
keytool -noprompt -keystore $OUTPUT_GEN/keystore-server.jks -alias selfsigned -import -file $OUTPUT_GEN/server_cert.pem -storepass $KEYSTORE_PASSWORD

## Import CA into client truststore
keytool -noprompt -keystore $OUTPUT_GEN/truststore-client.jks -alias CARoot -import -file $OUTPUT_GEN/ca_cert.pem -storepass $TRUSTSTORE_PASSWORD

## Import CA into server truststore
keytool -noprompt -keystore $OUTPUT_GEN/truststore-server.jks -alias CARoot -import -file $OUTPUT_GEN/ca_cert.pem -storepass $TRUSTSTORE_PASSWORD

cat $OUTPUT_GEN/client_cert.pem $OUTPUT_GEN/ca_cert.pem > $OUTPUT_GEN/ca_and_client_cert.pem
cat $OUTPUT_GEN/server_cert.pem $OUTPUT_GEN/ca_cert.pem > $OUTPUT_GEN/ca_and_server_cert.pem

else

echo "Secrets folder already exists. Just copying files..."

fi

DST=$OUTPUT_ENV/keystores

mkdir -p $DST

cp $OUTPUT_GEN/keystore-auth.jceks $DST
cp $OUTPUT_GEN/keystore-client.jks $DST
cp $OUTPUT_GEN/keystore-server.jks $DST
cp $OUTPUT_GEN/truststore-client.jks $DST
cp $OUTPUT_GEN/truststore-server.jks $DST

DST=$OUTPUT_ENV/nginx

mkdir -p $DST

cp $OUTPUT_GEN/ca_cert.pem $DST
cp $OUTPUT_GEN/server_cert.pem $DST
cp $OUTPUT_GEN/server_key.pem $DST
cp $OUTPUT_GEN/ca_and_server_cert.pem $DST
