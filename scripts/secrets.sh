#!/bin/bash

set -e

SAN=${1:-"san=dns:localhost"}

echo $SAN

OUTPUT=secrets

rm -fR $OUTPUT

mkdir -p $OUTPUT

echo "Create keystore for JWT authentication"
keytool -genseckey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass secret
keytool -genseckey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass secret
keytool -genseckey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass secret
keytool -genkey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg RSA -keysize 2048 -alias RS256 -keypass secret -sigalg SHA256withRSA -dname "CN=blueprint" -validity 365
keytool -genkey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg RSA -keysize 2048 -alias RS384 -keypass secret -sigalg SHA384withRSA -dname "CN=blueprint" -validity 365
keytool -genkey -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg RSA -keysize 2048 -alias RS512 -keypass secret -sigalg SHA512withRSA -dname "CN=blueprint" -validity 365
keytool -genkeypair -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg EC -groupname secp256r1 -alias ES256 -keypass secret -sigalg SHA256withECDSA -dname "CN=blueprint" -validity 365
keytool -genkeypair -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg EC -groupname secp256r1 -alias ES384 -keypass secret -sigalg SHA384withECDSA -dname "CN=blueprint" -validity 365
keytool -genkeypair -keystore $OUTPUT/keystore_auth.jceks -storetype JCEKS -storepass secret -keyalg EC -groupname secp256r1 -alias ES512 -keypass secret -sigalg SHA512withECDSA -dname "CN=blueprint" -validity 365

#echo "Create certificate authority (CA)"
#keytool -noprompt -keystore $OUTPUT/keystore_ca.jks -genkeypair -alias ca -dname "CN=blueprint" -ext KeyUsage=digitalSignature,keyCertSign -ext BasicConstraints=ca:true,PathLen:3 -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass secret -keypass secret
#openssl pkcs12 -in $OUTPUT/keystore_ca.jks -nocerts -nodes -passin pass:secret -out $OUTPUT/ca_key.pem
#openssl pkcs12 -in $OUTPUT/keystore_ca.jks -nokeys -nodes -passin pass:secret -out $OUTPUT/ca_cert.pem
#
#echo "Create client keystore"
#keytool -noprompt -keystore $OUTPUT/keystore_client.jks -genkeypair -alias client -dname "CN=blueprint" -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass secret -keypass secret
#
#echo "Create server keystore"
#keytool -noprompt -keystore $OUTPUT/keystore_server.jks -genkeypair -alias server -dname "CN=blueprint" -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass secret -keypass secret
#
#echo "Sign client certificate"
#keytool -noprompt -keystore $OUTPUT/keystore_client.jks -alias client -certreq -file $OUTPUT/client_csr.pem -storepass secret
#keytool -noprompt -keystore $OUTPUT/keystore_ca.jks -alias ca -gencert -infile $OUTPUT/client_csr.pem -outfile $OUTPUT/client_cert.pem -sigalg SHA256withRSA -ext KeyUsage=digitalSignature,keyAgreement -ext ExtendedKeyUsage=serverAuth,clientAuth -ext $SAN -rfc -validity 365 -storepass secret -keypass secret
#
#echo "Sign server certificate"
#keytool -noprompt -keystore $OUTPUT/keystore_server.jks -alias server -certreq -file $OUTPUT/server_csr.pem -storepass secret
#keytool -noprompt -keystore $OUTPUT/keystore_ca.jks -alias ca -gencert -infile $OUTPUT/server_csr.pem -outfile $OUTPUT/server_cert.pem -sigalg SHA256withRSA -ext KeyUsage=digitalSignature,keyAgreement -ext ExtendedKeyUsage=serverAuth,clientAuth -ext $SAN -rfc -validity 365 -storepass secret -keypass secret
#
#echo "Import CA and client signed certificate into client keystore"
#keytool -noprompt -keystore $OUTPUT/keystore_client.jks -alias ca -import -file $OUTPUT/ca_cert.pem -storepass secret
#keytool -noprompt -keystore $OUTPUT/keystore_client.jks -alias client -import -file $OUTPUT/client_cert.pem -storepass secret
#
#echo "Import CA and server signed certificate into server keystore"
#keytool -noprompt -keystore $OUTPUT/keystore_server.jks -alias ca -import -file $OUTPUT/ca_cert.pem -storepass secret
#keytool -noprompt -keystore $OUTPUT/keystore_server.jks -alias server -import -file $OUTPUT/server_cert.pem -storepass secret
#
#echo "Import CA into client truststore"
#keytool -noprompt -keystore $OUTPUT/truststore_client.jks -alias ca -import -file $OUTPUT/ca_cert.pem -storepass secret
#
#echo "Import CA into server truststore"
#keytool -noprompt -keystore $OUTPUT/truststore_server.jks -alias ca -import -file $OUTPUT/ca_cert.pem -storepass secret
#
#echo "Export certificates"
#openssl pkcs12 -in $OUTPUT/keystore_client.jks -nocerts -nodes -passin pass:secret -out $OUTPUT/client_key.pem
#openssl pkcs12 -in $OUTPUT/keystore_server.jks -nocerts -nodes -passin pass:secret -out $OUTPUT/server_key.pem
#
#echo "Create NGINX keystore"
#keytool -noprompt -keystore $OUTPUT/keystore_nginx.jks -genkeypair -alias nginx -dname "CN=blueprint" -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 365 -storepass secret -keypass secret
#
#echo "Sign NGINX certificate"
#keytool -noprompt -keystore $OUTPUT/keystore_nginx.jks -alias nginx -certreq -file $OUTPUT/nginx_csr.pem -storepass secret
#keytool -noprompt -keystore $OUTPUT/keystore_ca.jks -alias ca -gencert -infile $OUTPUT/nginx_csr.pem -outfile $OUTPUT/nginx_cert.pem -sigalg SHA256withRSA -ext KeyUsage=digitalSignature,keyAgreement -ext ExtendedKeyUsage=serverAuth,clientAuth -ext $SAN -rfc -validity 365 -storepass secret -keypass secret
#
#echo "Export NGINX key"
#openssl pkcs12 -in $OUTPUT/keystore_nginx.jks -nocerts -nodes -passin pass:secret -out $OUTPUT/nginx_key.pem
#
#echo "Create NGINX certificates"
#cat $OUTPUT/nginx_key.pem > $OUTPUT/nginx_server_key.pem
#cat $OUTPUT/nginx_cert.pem > $OUTPUT/nginx_server_cert.pem
#cat $OUTPUT/ca_cert.pem >> $OUTPUT/nginx_server_cert.pem
