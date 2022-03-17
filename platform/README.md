# event-sourcing-cqrs-example

TODO

## Requirements

Generate secrets:

    ./scripts/secrets.sh

## Build on Docker

Start pipeline:

    docker compose -f docker-compose-pipeline.yaml -p pipeline up -d

Export Nexus password:

    export NEXUS_PASSWORD=$(docker exec $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository (required only once):

    ./scripts/create-repository.sh --nexus-host=localhost --nexus-port=38081 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} 

Build platform:

    ./scripts/build-platform.sh --nexus-host=localhost --nexus-port=38081 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} 

Stop pipeline (when finished):

    docker compose -f docker-compose-pipeline.yaml -p pipeline down

Update dependencies (if needed):

    mvn versions:update-properties -Dcommon=true -Dservices=true -Dplatform=true
    mvn versions:commit

## Run on Docker

Export GitHub secrets (very important):

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Start services:

    docker compose -f docker-compose-services.yaml -p services up -d

Create Kafka topics:

    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic designs --config "retention.ms=604800000" --replication-factor=1 --partitions=16

    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-0 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-1 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-2 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-3 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-4 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64

    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic batch --config "retention.ms=604800000" --replication-factor=1 --partitions=16

Start platform:

    docker compose -f docker-compose-platform.yaml -p platform up -d

Open browser:

    open https://localhost:8080/browse/designs.html

Stop platform (when finished):

    docker compose -f docker-compose-platform.yaml -p platform down

Stop services (when finished):

    docker compose -f docker-compose-services.yaml -p services down

## Build on Minikube

Setup Minikube:

    minikube start --vm-driver=hyperkit --cpus 8 --memory 48000m —disk-size 200g --kubernetes-version v1.22.2

Create namespace:

    kubectl create ns blueprint

Deploy Nexus:

    helm install integration-nexus helm/nexus -n blueprint --set replicas=1

Check Nexus:

    kubectl -n blueprint logs -f --tail=-1 -l app=nexus

Deploy Postgres:

    helm install integration-postgres helm/postgres -n blueprint --set replicas=1

Check Postgres:

    kubectl -n blueprint logs -f --tail=-1 -l app=postgres

Deploy Pact Broker:

    helm install integration-pactbroker helm/pactbroker -n blueprint --set replicas=1

Check Pact Broker:

    kubectl -n blueprint logs -f --tail=-1 -l app=pactbroker

Expose services:

    kubectl -n blueprint expose service/nexus --name nexus-external --port 8081 --target-port 8081 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/pactbroker --name pactbroker-external --port 9292 --target-port 9292 --type LoadBalancer --external-ip $(minikube ip)

Export variables:

    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n blueprint exec $(kubectl -n blueprint get pod -l app=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository (required only once):

    ./scripts/create-repository.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

Configure Docker:

    eval $(minikube docker-env)

Build platform:

    ./scripts/build-platform.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD} --docker-host=172.17.0.1

Upgrade Nexus (if needed):

    helm upgrade --install integration-nexus helm/nexus -n blueprint --set replicas=1

Upgrade Postgres (if needed):

    helm upgrade --install integration-postgres helm/postgres -n blueprint --set replicas=1

Upgrade Pact Broker (if needed):

    helm upgrade --install integration-pactbroker helm/pactbroker -n blueprint --set replicas=1

## Run on Minikube

Setup Minikube:

    minikube addons enable metrics-server
    minikube addons enable dashboard
    minikube addons enable registry

Deploy secrets:

    kubectl -n blueprint create secret generic keystore-server.jks --from-file=secrets/keystore_server.jks
    kubectl -n blueprint create secret generic keystore-client.jks --from-file=secrets/keystore_client.jks
    kubectl -n blueprint create secret generic truststore-server.jks --from-file=secrets/truststore_server.jks
    kubectl -n blueprint create secret generic truststore-client.jks --from-file=secrets/truststore_client.jks
    kubectl -n blueprint create secret generic keystore-auth.jceks --from-file=secrets/keystore_auth.jceks
    kubectl -n blueprint create secret generic nginx --from-file server_cert.pem=secrets/nginx_server_cert.pem --from-file server_key.pem=secrets/nginx_server_key.pem

Deploy Jaeger:

    helm install integration-jaeger helm/jaeger -n blueprint --set replicas=1

Check Jaeger:

    kubectl -n blueprint logs -f --tail=-1 -l app=jaeger

Deploy Kibana:

    helm install integration-kibana helm/kibana -n blueprint --set replicas=1

Check Kibana:

    kubectl -n blueprint logs -f --tail=-1 -l app=kibana

Deploy Elasticsearch:

    helm install integration-elasticsearch helm/elasticsearch -n blueprint --set replicas=1

Check Elasticsearch:

    kubectl -n blueprint logs -f --tail=-1 -l app=elasticsearch

Deploy Cassandra:

    helm install integration-cassandra helm/cassandra -n blueprint --set replicas=1

Check Cassandra:

    kubectl -n blueprint logs -f --tail=-1 -l app=cassandra

Deploy Zookeeper:

    helm install integration-zookeeper helm/zookeeper -n blueprint --set replicas=1

Check Zookeeper:

    kubectl -n blueprint logs -f --tail=-1 -l app=zookeeper

Deploy Kafka:

    helm install integration-kafka helm/kafka -n blueprint --set replicas=1,externalName=$(minikube ip):9093

Check Kafka:

    kubectl -n blueprint logs -f --tail=-1 -l app=kafka

Deploy MySQL:

    helm install integration-mysql helm/mysql -n blueprint --set replicas=1

Check MySQL:

    kubectl -n blueprint logs -f --tail=-1 -l app=mysql

Deploy NGINX:

    helm install integration-nginx helm/nginx -n blueprint --set replicas=1

Check NGINX:

    kubectl -n blueprint logs -f --tail=-1 -l app=nginx

Deploy Consul:

    helm install integration-consul helm/consul -n blueprint --set replicas=1,servicePort=8000,serviceName=$(minikube ip)

Check Consul:

    kubectl -n blueprint logs -f --tail=-1 -l app=consul

Deploy Minio:

    helm install integration-minio helm/minio -n blueprint --set replicas=1

Check Minio:

    kubectl -n blueprint logs -f --tail=-1 -l app=minio

Expose servers:

    kubectl -n blueprint expose service/kibana --name=kibana-external --port=5601 --target-port=5601 --type=LoadBalancer --external-ip=$(minikube ip) 
    kubectl -n blueprint expose service/jaeger --name=jaeger-external --port=16686 --target-port=16686 --type=LoadBalancer --external-ip=$(minikube ip)
    kubectl -n blueprint expose service/consul --name=consul-external --port=8500 --target-port=8500 --type=LoadBalancer --external-ip=$(minikube ip)
    kubectl -n blueprint expose service/minio --name minio-external --port 9001 --target-port 9001 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n blueprint expose service/nginx --name nginx-external --port 443 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)

Export GitHub secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Deploy secrets for services:

    kubectl -n blueprint create secret generic authentication --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal GITHUB_ACCOUNT_EMAIL=$GITHUB_ACCOUNT_EMAIL --from-literal GITHUB_CLIENT_ID=$GITHUB_CLIENT_ID --from-literal GITHUB_CLIENT_SECRET=$GITHUB_CLIENT_SECRET

    kubectl -n blueprint create secret generic accounts --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password

    kubectl -n blueprint create secret generic designs-query --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password
    kubectl -n blueprint create secret generic designs-command --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-aggregate --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n blueprint create secret generic designs-notify --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret 
    kubectl -n blueprint create secret generic designs-render --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password

    kubectl -n blueprint create secret generic gateway --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret

    kubectl -n blueprint create secret generic frontend --from-file ca_cert.pem=secrets/ca_cert.pem --from-file server_cert.pem=secrets/server_cert.pem --from-file server_key.pem=secrets/server_key.pem

Export version:

     export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)

Deploy services:

    helm install service-authentication services/authentication/helm -n blueprint --set image.repository=integration/authentication,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):443,clientAuthUrl=https://$(minikube ip):443

    helm install service-accounts services/accounts/helm -n blueprint --set image.repository=integration/accounts,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm install service-designs-query services/designs-query/helm -n blueprint --set image.repository=integration/designs-query,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-command services/designs-command/helm -n blueprint --set image.repository=integration/designs-command,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-aggregate services/designs-aggregate/helm -n blueprint --set image.repository=integration/designs-aggregate,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-notify services/designs-notify/helm -n blueprint --set image.repository=integration/designs-notify,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm install service-designs-render services/designs-render/helm -n blueprint --set image.repository=integration/designs-render,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm install service-gateway services/gateway/helm -n blueprint --set image.repository=integration/gateway,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm install service-frontend services/frontend/helm -n blueprint --set image.repository=integration/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=https://$(minikube ip):443,clientApiUrl=https://$(minikube ip):443

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

Expose services:

    kubectl -n blueprint expose service/designs-notify --name designs-notify-external --port 8000 --target-port 8080 --type LoadBalancer --external-ip $(minikube ip)

Scale services (if needed):

    kubectl -n blueprint scale deployment authentication --replicas=2
    kubectl -n blueprint scale deployment accounts --replicas=2
    kubectl -n blueprint scale deployment designs-command --replicas=2
    kubectl -n blueprint scale deployment designs-aggregate --replicas=4
    kubectl -n blueprint scale deployment designs-query --replicas=2
    kubectl -n blueprint scale deployment designs-render --replicas=8
    kubectl -n blueprint scale deployment frontend --replicas=2
    kubectl -n blueprint scale deployment gateway --replicas=2
    kubectl -n blueprint scale deployment nginx --replicas=2

Open browser:

    open https://$(minikube ip)/browse/designs.html

Login with your GitHub account associated with the admin email for getting admin access

Upgrade services (if needed):

    helm upgrade --install service-authentication services/authentication/helm -n blueprint --set image.repository=integration/authentication,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):443,clientAuthUrl=https://$(minikube ip):443

    helm upgrade --install service-accounts services/accounts/helm -n blueprint --set image.repository=integration/accounts,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm upgrade --install service-designs-query services/designs-query/helm -n blueprint --set image.repository=integration/designs-query,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm upgrade --install service-designs-command services/designs-command/helm -n blueprint --set image.repository=integration/designs-command,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm upgrade --install service-designs-aggregate services/designs-aggregate/helm -n blueprint --set image.repository=integration/designs-aggregate,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm upgrade --install service-designs-notify services/designs-notify/helm -n blueprint --set image.repository=integration/designs-notify,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)
    helm upgrade --install service-designs-render services/designs-render/helm -n blueprint --set image.repository=integration/designs-render,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm upgrade --install service-gateway services/gateway/helm -n blueprint --set image.repository=integration/gateway,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip)

    helm upgrade --install service-frontend services/frontend/helm -n blueprint --set image.repository=integration/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=https://$(minikube ip):443,clientApiUrl=https://$(minikube ip):443

Upgrade Jaeger (if needed):

    helm upgrade --install integration-jaeger helm/jaeger -n blueprint --set replicas=1

Upgrade Kibana (if needed):

    helm upgrade --install integration-kibana helm/kibana -n blueprint --set replicas=1

Upgrade Elasticsearch (if needed):

    helm upgrade --install integration-elasticsearch helm/elasticsearch -n blueprint --set replicas=1

Upgrade Cassandra (if needed):

    helm upgrade --install integration-cassandra helm/cassandra -n blueprint --set replicas=1

Upgrade Zookeeper (if needed):

    helm upgrade --install integration-zookeeper helm/zookeeper -n blueprint --set replicas=1

Upgrade Kafka (if needed):

    helm upgrade --install integration-kafka helm/kafka -n blueprint --set replicas=1,externalName=$(minikube ip):9093

Upgrade MySQL (if needed):

    helm upgrade --install integration-mysql helm/mysql -n blueprint --set replicas=1

Upgrade NGINX (if needed):

    helm upgrade --install integration-nginx helm/nginx -n blueprint --set replicas=1

Upgrade Consul (if needed):

    helm upgrade --install integration-consul helm/consul -n blueprint --set replicas=1,servicePort=8000,serviceName=$(minikube ip)

Upgrade Minio (if needed):

    helm upgrade --install integration-minio helm/minio -n blueprint --set replicas=1

Stop Minikube (when finished):

    minikube stop






/////////////////

docker run -it --network services_services -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest -c "mc config host add integration http://minio:9000 admin password && mc rm -r --force integration/tiles && mc mb integration/tiles"

docker run -it --network services_services -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest -c "mc config host add integration http://minio:9000 admin password && mc mb integration/tiles"

mvn -s settings.xml -Dcommon=true -Dservices=true -Dplatform=true -Dnexus=true -DskipTests=true -Dnexus.host=localhost -Dnexus.port=38081 -Dpactbroker.host=localhost -Dpactbroker.port=9292 clean package

minikube start --mount-string "$(pwd)/scripts:/var/docker/scripts" --mount

/////////////////

Only one replica per partition is allowed for designs-command-consumer. Only one replica per node is allowed for designs-notify.


