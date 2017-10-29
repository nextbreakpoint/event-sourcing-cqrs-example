keytool -genkey -alias fractals -dname "CN=NextBreakpoint" -keystore keystore-server.jks -keyalg RSA -keysize 2048 -keypass "secret" -storepass "secret" -storetype JKS -validity 999
keytool -genkey -alias fractals -dname "CN=NextBreakpoint" -keystore keystore-client.jks -keyalg RSA -keysize 2048 -keypass "secret" -storepass "secret" -storetype JKS -validity 999
keytool -export -keystore keystore-server.jks -alias fractals -file server.cer -storepass "secret"
keytool -export -keystore keystore-client.jks -alias fractals -file client.cer -storepass "secret"
keytool -import -alias fractals -file server.cer -keystore truststore-client.jks -storepass "secret" -noprompt
keytool -import -alias fractals -file client.cer -keystore truststore-server.jks -storepass "secret" -noprompt
