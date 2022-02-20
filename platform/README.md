# event-sourcing-cqrs-example

TODO

## Requirements

Generate secrets:

    ./scripts/secrets.sh

## Build on Docker

Start pipeline:

    docker compose -f docker-compose-pipeline.yaml -p pipeline up -d

Export variables:

    export NEXUS_HOST=localhost
    export NEXUS_PORT=38081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(docker exec $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    curl -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X POST "http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted" -H "accept: application/json" -H "Content-Type: application/json" -d "{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}"

Build platform:

    ./scripts/build.sh 

Stop pipeline (when finished):

    docker compose -f docker-compose-pipeline.yaml -p pipeline down

## Run on Docker

Export GitHub secrets:

    export GITHUB_ACCOUNT_ID=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Start services:

    docker compose -f docker-compose-services.yaml -p services up -d

Create Kafka topics:

    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic design-event --config "retention.ms=604800000" --replication-factor=1 --partitions=16
    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic tiles-rendering-queue --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64

Start platform:

    docker compose -f docker-compose-platform.yaml -p platform up -d

Open browser:

    open https://localhost:8080/designs/designs.html

Stop platform (when finished):

    docker compose -f docker-compose-platform.yaml -p platform down

Stop services (when finished):

    docker compose -f docker-compose-services.yaml -p services down

## Build on Minikube

Setup Minikube:

    minikube start --vm-driver=hyperkit --cpus 8 --memory 32768m —disk-size 100g --kubernetes-version v1.22.2

    minikube start --mount-string "$(pwd)/scripts:/var/docker/scripts" --mount

Deploy Nexus:

    helm install integration-nexus platform/helm/nexus -n blueprint --set replicas=1

Check Nexus:

    kubectl -n blueprint logs -f --tail=-1 -l app=nexus

Deploy Postgres:

    helm install integration-postgres platform/helm/postgres -n blueprint --set replicas=1

Check Postgres:

    kubectl -n blueprint logs -f --tail=-1 -l app=postgres

Deploy Pact Broker:

    helm install integration-pactbroker platform/helm/pactbroker -n blueprint --set replicas=1

Check Pact Broker:

    kubectl -n blueprint logs -f --tail=-1 -l app=pactbroker

Export variables:

    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n blueprint exec $(kubectl -n blueprint get pod -l app=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    curl -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X POST "http://${NEXUS_HOST}:${NEXUS_PORT}/service/rest/v1/repositories/maven/hosted" -H "accept: application/json" -H "Content-Type: application/json" -d "{ \"name\": \"maven-internal\", \"online\": true, \"storage\": { \"blobStoreName\": \"default\", \"strictContentTypeValidation\": true, \"writePolicy\": \"allow_once\" }, \"cleanup\": { \"policyNames\": [ \"string\" ] }, \"component\": { \"proprietaryComponents\": true }, \"maven\": { \"versionPolicy\": \"MIXED\", \"layoutPolicy\": \"STRICT\" }}"

Configure Docker:

    eval $(minikube docker-env)

Build platform:

    ./scripts/build.sh 

## Run on Minikube

Setup Minikube:

    minikube addons enable metrics-server
    minikube addons enable dashboard
    minikube addons enable registry

Create namespace:

    kubectl create ns blueprint

Deploy secrets:

    kubectl -n blueprint create secret generic keystore-server.jks --from-file=secrets/keystore_server.jks
    kubectl -n blueprint create secret generic keystore-client.jks --from-file=secrets/keystore_client.jks
    kubectl -n blueprint create secret generic truststore-server.jks --from-file=secrets/truststore_server.jks
    kubectl -n blueprint create secret generic truststore-client.jks --from-file=secrets/truststore_client.jks
    kubectl -n blueprint create secret generic keystore-auth.jceks --from-file=secrets/keystore_auth.jceks
    kubectl -n blueprint create secret generic nginx --from-file server_cert.pem=secrets/nginx_server_cert.pem --from-file server_key.pem=secrets/nginx_server_key.pem

Deploy Elasticsearch:

    helm install integration-elasticsearch platform/helm/elasticsearch -n blueprint --set replicas=1

Check Elasticsearch:

    kubectl -n blueprint logs -f --tail=-1 -l app=elasticsearch

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

Deploy Minio:

    helm install integration-minio platform/helm/minio -n blueprint --set replicas=1

Check Minio:

    kubectl -n blueprint logs -f --tail=-1 -l app=minio

Export GitHub secrets:

    export GITHUB_ACCOUNT_ID=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Deploy secrets for services:

    kubectl -n blueprint create secret generic authentication --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal GITHUB_ACCOUNT_ID=$GITHUB_ACCOUNT_ID --from-literal GITHUB_CLIENT_ID=$GITHUB_CLIENT_ID --from-literal GITHUB_CLIENT_SECRET=$GITHUB_CLIENT_SECRET

    kubectl -n blueprint create secret generic accounts --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password

    kubectl -n blueprint create secret generic designs-query --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-command --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-aggregate --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-notify --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password
    kubectl -n blueprint create secret generic designs-render --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password

    kubectl -n blueprint create secret generic gateway --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret

    kubectl -n blueprint create secret generic frontend --from-file ca_cert.pem=secrets/ca_cert.pem --from-file server_cert.pem=secrets/server_cert.pem --from-file server_key.pem=secrets/server_key.pem

Deploy services:

    helm install service-authentication platform/services/authentication/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):8081,clientAuthUrl=https://$(minikube ip):8081

    helm install service-accounts platform/services/accounts/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)

    helm install service-designs-query platform/services/designs-query/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-command platform/services/designs-command/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-aggregate platform/services/designs-aggregate/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-notify platform/services/designs-notify/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-render platform/services/designs-render/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)

    helm install service-gateway platform/services/gateway/helm -n blueprint --set replicas=1,clientDomain=$(minikube ip)

    helm install service-frontend platform/services/frontend/helm -n blueprint --set replicas=1,clientWebUrl=https://$(minikube ip):8081,clientApiUrl=https://$(minikube ip):8081

Check services:

    kubectl -n blueprint logs -f --tail=-1 -l app=authentication
    kubectl -n blueprint logs -f --tail=-1 -l app=accounts
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-query
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-command
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-aggregate
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-notify
    kubectl -n blueprint logs -f --tail=-1 -l app=designs-render
    kubectl -n blueprint logs -f --tail=-1 -l app=gateway
    kubectl -n blueprint logs -f --tail=-1 -l app=frontend

Forward ports (if needed):

    kubectl -n blueprint expose service/designs-notify --name designs-notify-external --port 30080 --target-port 8080 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/nginx --name nginx-external --port 443 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/minio --name minio-external --port 9000 --target-port 9000 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/nexus --name nexus-external --port 8081 --target-port 8081 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/pactbroker --name pactbroker-external --port 9292 --target-port 9292 --type LoadBalancer --external-ip $(minikube ip)

Scale services (if needed):

    kubectl -n blueprint scale deployment authentication --replicas=2
    kubectl -n blueprint scale deployment accounts --replicas=2
    kubectl -n blueprint scale deployment designs-command --replicas=2
    kubectl -n blueprint scale deployment designs-aggregate --replicas=2
    kubectl -n blueprint scale deployment designs-query --replicas=4
    kubectl -n blueprint scale deployment designs-render --replicas=8
    kubectl -n blueprint scale deployment frontend --replicas=2
    kubectl -n blueprint scale deployment gateway --replicas=2
    kubectl -n blueprint scale deployment nginx --replicas=4

Stop Minikube (when finished):

    minikube stop


/////////////////

Only one replica per partition is allowed for designs-command-consumer. Only one replica per node is allowed for designs-notify.

docker run -it --network services_services -e MINIO_ACCESS_KEY=admin -e MINIO_SECRET_KEY=password --entrypoint sh minio/mc:latest -c "mc config host add integration http://minio:9000 admin password && mc rm -r --force integration/tiles && mc mb integration/tiles"

docker run -it --network services_services -e MINIO_ACCESS_KEY=admin -e MINIO_SECRET_KEY=password --entrypoint sh minio/mc:latest -c "mc config host add integration http://minio:9000 admin password && mc mb integration/tiles"
