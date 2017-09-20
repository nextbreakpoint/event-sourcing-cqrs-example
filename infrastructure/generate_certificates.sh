#!/bin/bash
export DIR=terraform/services/environments/production

keytool -genseckey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass secret
keytool -genseckey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass secret
keytool -genseckey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass secret
keytool -genkey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS256 -keypass secret -sigalg SHA256withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS384 -keypass secret -sigalg SHA384withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS512 -keypass secret -sigalg SHA512withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES256 -keypass secret -sigalg SHA256withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES384 -keypass secret -sigalg SHA384withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore $DIR/keystores/keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES512 -keypass secret -sigalg SHA512withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360

keytool -genkey -alias fractals -dname "CN=NextBreakpoint" -keystore $DIR/keystores/keystore-server.jks -keyalg RSA -keysize 2048 -keypass "secret" -storepass "secret" -storetype JKS -validity 999
keytool -genkey -alias fractals -dname "CN=NextBreakpoint" -keystore $DIR/keystores/keystore-client.jks -keyalg RSA -keysize 2048 -keypass "secret" -storepass "secret" -storetype JKS -validity 999
keytool -export -keystore $DIR/keystores/keystore-server.jks -alias fractals -file $DIR/keystores/server.cer -storepass "secret"
keytool -export -keystore $DIR/keystores/keystore-client.jks -alias fractals -file $DIR/keystores/client.cer -storepass "secret"
keytool -import -alias fractals -file $DIR/keystores/server.cer -keystore $DIR/keystores/truststore-client.jks -storepass "secret" -noprompt
keytool -import -alias fractals -file $DIR/keystores/client.cer -keystore $DIR/keystores/truststore-server.jks -storepass "secret" -noprompt
