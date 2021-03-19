# event-sourcing-cqrs-example

Generate secrets:

    ./scripts/secrets.sh

Create namespace:

    kubectl create ns blueprint

Deploy secrets:

    kubectl -n blueprint create secret generic keystore-server.jks --from-file=secrets/keystore-server.jks
    kubectl -n blueprint create secret generic keystore-client.jks --from-file=secrets/keystore-client.jks
    kubectl -n blueprint create secret generic truststore-server.jks --from-file=secrets/truststore-server.jks
    kubectl -n blueprint create secret generic truststore-client.jks --from-file=secrets/truststore-client.jks
    kubectl -n blueprint create secret generic keystore-auth.jceks --from-file=secrets/keystore-auth.jceks
    kubectl -n blueprint create secret generic nginx --from-file server_cert.pem=secrets/nginx_server_cert.pem --from-file server_key.pem=secrets/nginx_server_key.pem

Build Docker images:

    ./build_images.sh

Deploy Cassandra:

    helm install integration-cassandra platform/helm/cassandra -n blueprint --set replicas=1

Check Cassandra:

    kubectl -n blueprint logs -f --tail=-1 -l app=cassandra

Deploy Zookeeper:

    helm install integration-zookeeper platform/helm/zookeeper -n blueprint --set replicas=1

Check Zookeeper:

    kubectl -n blueprint logs -f --tail=-1 -l app=zookeeper

Deploy Kafka:

    helm install integration-kafka platform/helm/kafka -n blueprint --set replicas=1

Check Kafka:

    kubectl -n blueprint logs -f --tail=-1 -l app=kafka

Deploy MySQL:

    helm install integration-mysql platform/helm/mysql -n blueprint --set replicas=1

Check MySQL:

    kubectl -n blueprint logs -f --tail=-1 -l app=mysql

Deploy NGINX:

    helm install integration-nginx platform/helm/nginx -n blueprint --set replicas=1

Check NGINX:

    kubectl -n blueprint logs -f --tail=-1 -l app=nginx

Deploy Consul:

    helm install integration-consul platform/helm/consul -n blueprint --set replicas=1,servicePort=30080,serviceName=$(minikube ip)

Check Consul:

    kubectl -n blueprint logs -f --tail=-1 -l app=consul

Build services:

    ./build_services.sh

Export GitHub secrets:

    export GITHUB_ACCOUNT_ID=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Deploy secrets for services:

    kubectl -n blueprint create secret generic authentication --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal GITHUB_ACCOUNT_ID=$GITHUB_ACCOUNT_ID --from-literal GITHUB_CLIENT_ID=$GITHUB_CLIENT_ID --from-literal GITHUB_CLIENT_SECRET=$GITHUB_CLIENT_SECRET

    kubectl -n blueprint create secret generic accounts --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password

    kubectl -n blueprint create secret generic designs-aggregate-fetcher --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-command-consumer --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-command-producer --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret  
    kubectl -n blueprint create secret generic designs-notification-dispatcher --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret  

    kubectl -n blueprint create secret generic gateway --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret

    kubectl -n blueprint create secret generic frontend --from-file ca_cert.pem=secrets/ca_cert.pem --from-file server_cert.pem=secrets/server_cert.pem --from-file server_key.pem=secrets/server_key.pem

Deploy services:

    helm install service-authentication platform/services/authentication/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):8081,clientAuthUrl=https://$(minikube ip):8081
    helm install service-accounts platform/services/accounts/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs platform/services/designs/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-aggregate-fetcher platform/services/designs-aggregate-fetcher/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-command-consumer platform/services/designs-command-consumer/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-command-producer platform/services/designs-command-producer/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-notification-dispatcher platform/services/designs-notification-dispatcher/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)

    helm install service-gateway platform/services/gateway/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)

    helm install service-frontend platform/services/frontend/helm -n blueprint --set replicas=1,clientWebUrl=https://$(minikube ip):8081,clientApiUrl=https://$(minikube ip):8081

Check services:

    kubectl -n blueprint logs -f --tail=-1 -l app=authentication
    kubectl -n blueprint logs -f --tail=-1 -l app=accounts
    kubectl -n blueprint logs -f --tail=-1 -l app=designs
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-aggregate-fetcher
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-command-consumer
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-command-producer
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-notification-dispatcher
    kubectl -n blueprint logs -f --tail=-1 -l app=gateway
    kubectl -n blueprint logs -f --tail=-1 -l app=frontend

Forward ports:

    kubectl -n blueprint expose service/designs-notification-dispatcher --name designs-notification-dispatcher-external --port 30080 --target-port 8080 --type LoadBalancer --external-ip $(minikube ip)

    kubectl -n blueprint expose service/nginx --name nginx-external --port 443 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)

Scale services:

    kubectl -n blueprint scale deployment authentication --replicas=2
    kubectl -n blueprint scale deployment accounts --replicas=2
    kubectl -n blueprint scale deployment designs --replicas=4
    kubectl -n blueprint scale deployment designs-command-producer --replicas=2
    kubectl -n blueprint scale deployment designs-aggregate-fetcher --replicas=4
    kubectl -n blueprint scale deployment frontend --replicas=2
    kubectl -n blueprint scale deployment gateway --replicas=2
    kubectl -n blueprint scale deployment nginx --replicas=4

Only one replica per partition is allowed for designs-command-consumer.
Only one replica per node is allowed for designs-notification-dispatcher.


docker exec -it $(docker container ls -f name=platform_nexus -q) cat /nexus-data/admin.password

export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=$(docker exec -it $(docker container ls -f name=platform_nexus -q) cat /nexus-data/admin.password)

curl -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X POST "http://192.168.64.12:38081/service/rest/v1/repositories/maven/hosted" -H "accept: application/json" -H "Content-Type: application/json" -d "{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}"
