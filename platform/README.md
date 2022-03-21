# event-sourcing-cqrs-example

TODO

## Requirements

Generate secrets:

    ./scripts/secrets.sh

## Build on Docker

Start pipeline:

    docker compose -f docker-compose-pipeline.yaml -p pipeline up -d

Export Nexus password (wait until Nexus has started):

    export NEXUS_PASSWORD=$(docker exec $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository (required only once):

    ./scripts/create-repository.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} 

Build platform:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} 

See results of Pact tests:

    open http://localhost:9292

Build platform without tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests 

Build platform and run tests but skip Pact tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-pact-tests --skip-pact-verify 

Run tests without building:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --skip-deploy --version=${VERSION} 

Run only Pact tests without building:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --skip-deploy --version=${VERSION} --skip-integration-tests 

Stop pipeline (when finished):

    docker compose -f docker-compose-pipeline.yaml -p pipeline down

Update dependencies (if needed):

    mvn versions:update-properties -Dcommon=true -Dservices=true -Dplatform=true
    mvn versions:commit

## Run on Docker

Build Docker images:

    ./scripts/build-images.sh 

Start platform:

    docker compose -f docker-compose-platform.yaml -p platform up -d

Create Kafka topics:

    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic events --config "retention.ms=604800000" --replication-factor=1 --partitions=16

    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-0 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-1 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-2 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-3 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    docker exec -i $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic render-4 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64

    docker exec -it $(docker ps | grep kafka | awk '{print $1}') kafka-topics --bootstrap-server=localhost:9092 --create --topic batch --config "retention.ms=604800000" --replication-factor=1 --partitions=16

Create Minio bucket:

    docker run -i --network platform_services -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest < scripts/minio-init.sh

See Jaeger console:

    open http://localhost:16686

See Kibana console:

    open http://localhost:5601

See Consul console:

    open http://localhost:8500

See Minio console:

    open http://localhost:9091

Login with user 'admin' and password 'password'.

Export GitHub secrets (very important):

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Start services:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
    docker compose -f docker-compose-services.yaml -p services up -d

Open browser:

    open https://localhost:8080/browse/designs.html

Stop services (when finished):

    docker compose -f docker-compose-services.yaml -p services down

Stop platform (when finished):

    docker compose -f docker-compose-platform.yaml -p platform down

## Build on Minikube

Setup Minikube:

    minikube start --vm-driver=hyperkit --cpus 8 --memory 32768m --disk-size 64g --kubernetes-version v1.22.2

Create alias (unless you already have kubectl installed):

    alias kubectl="minikube kubectl --"

Create namespaces:

    kubectl create ns pipeline
    kubectl create ns platform
    kubectl create ns services
    kubectl create ns monitoring

Deploy Nexus:

    helm install integration-nexus helm/nexus -n pipeline --set replicas=1

Check Nexus:

    kubectl -n pipeline logs -f --tail=-1 -l app=nexus

Deploy Postgres:

    helm install integration-postgres helm/postgres -n pipeline --set replicas=1

Check Postgres:

    kubectl -n pipeline logs -f --tail=-1 -l app=postgres

Deploy Pact Broker:

    helm install integration-pactbroker helm/pactbroker -n pipeline --set replicas=1

Check Pact Broker:

    kubectl -n pipeline logs -f --tail=-1 -l app=pactbroker

Expose services:

    kubectl -n pipeline expose service/nexus --name nexus-external --port 8081 --target-port 8081 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n pipeline expose service/pactbroker --name pactbroker-external --port 9292 --target-port 9292 --type LoadBalancer --external-ip $(minikube ip)

Wait until Nexus is ready:

     curl http://$(minikube ip):8081/#browse/browse:maven-public    

Export variables:

    export PACTBROKER_HOST=$(minikube ip)
    export PACTBROKER_PORT=9092
    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n pipeline exec $(kubectl -n pipeline get pod -l app=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository (required only once):

    ./scripts/create-repository.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

Configure Docker:

    eval $(minikube docker-env)

Build services:

    ./scripts/build-services.sh --pactbroker-host=${PACTBROKER_HOST} --pactbroker-port=${PACTBROKER_PORT} --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD} --docker-host=$(minikube ip) --skip-tests

Upgrade Nexus (if needed):

    helm upgrade --install integration-nexus helm/nexus -n pipeline --set replicas=1

Upgrade Postgres (if needed):

    helm upgrade --install integration-postgres helm/postgres -n pipeline --set replicas=1

Upgrade Pact Broker (if needed):

    helm upgrade --install integration-pactbroker helm/pactbroker -n pipeline --set replicas=1

## Run on Minikube

Setup Minikube:

    minikube addons enable metrics-server
    minikube addons enable dashboard
    minikube addons enable registry

Deploy secrets:

    kubectl -n platform create secret generic nginx --from-file server_cert.pem=secrets/nginx_server_cert.pem --from-file server_key.pem=secrets/nginx_server_key.pem
    
    kubectl -n services create secret generic keystore-server.jks --from-file=secrets/keystore_server.jks
    kubectl -n services create secret generic keystore-client.jks --from-file=secrets/keystore_client.jks
    kubectl -n services create secret generic truststore-server.jks --from-file=secrets/truststore_server.jks
    kubectl -n services create secret generic truststore-client.jks --from-file=secrets/truststore_client.jks
    kubectl -n services create secret generic keystore-auth.jceks --from-file=secrets/keystore_auth.jceks

Configure Docker:

    eval $(minikube docker-env)

Build Docker images:

    ./scripts/build-images.sh 

Create volumes:

    kubectl apply -f scripts/volumes.yaml 

Deploy Elasticsearch:

    helm install integration-elasticsearch helm/elasticsearch -n monitoring --set replicas=1,dataDirectory=/volumes/monitoring/elasticsearch-data

Check Elasticsearch:

    kubectl -n monitoring logs -f --tail=-1 -l app=elasticsearch

Deploy Kibana:

    helm install integration-kibana helm/kibana -n monitoring --set replicas=1,server.publicBaseUrl=http://$(minikube ip)::5601

Check Kibana:

    kubectl -n monitoring logs -f --tail=-1 -l app=kibana

Deploy Jaeger:

    helm install integration-jaeger helm/jaeger -n monitoring --set replicas=1

Check Jaeger:

    kubectl -n monitoring logs -f --tail=-1 -l app=jaeger

Deploy Elasticsearch:

    helm install integration-elasticsearch helm/elasticsearch -n platform --set replicas=1

Check Elasticsearch:

    kubectl -n platform logs -f --tail=-1 -l app=elasticsearch

Deploy Cassandra:

    helm install integration-cassandra helm/cassandra -n platform --set replicas=1

Check Cassandra:

    kubectl -n platform logs -f --tail=-1 -l app=cassandra

Deploy Zookeeper:

    helm install integration-zookeeper helm/zookeeper -n platform --set replicas=1

Check Zookeeper:

    kubectl -n platform logs -f --tail=-1 -l app=zookeeper

Deploy Kafka:

    helm install integration-kafka helm/kafka -n platform --set replicas=1,externalName=$(minikube ip):9093

Check Kafka:

    kubectl -n platform logs -f --tail=-1 -l app=kafka

Deploy MySQL:

    helm install integration-mysql helm/mysql -n platform --set replicas=1

Check MySQL:

    kubectl -n platform logs -f --tail=-1 -l app=mysql

Deploy Consul:

    helm install integration-consul helm/consul -n platform --set replicas=1,servicePort=8000,serviceName=$(minikube ip)

Check Consul:

    kubectl -n platform logs -f --tail=-1 -l app=consul

Deploy Minio:

    helm install integration-minio helm/minio -n platform --set replicas=1

Check Minio:

    kubectl -n platform logs -f --tail=-1 -l app=minio

Deploy NGINX:

    helm install integration-nginx helm/nginx -n platform --set replicas=1,hostname=$(minikube ip)

Check NGINX:

    kubectl -n platform logs -f --tail=-1 -l app=nginx

Expose servers:

    kubectl -n monitoring expose service/kibana --name=kibana-external --port=5601 --target-port=5601 --type=LoadBalancer --external-ip=$(minikube ip) 
    kubectl -n monitoring expose service/jaeger --name=jaeger-external --port=16686 --target-port=16686 --type=LoadBalancer --external-ip=$(minikube ip)
    kubectl -n platform expose service/consul --name=consul-external --port=8500 --target-port=8500 --type=LoadBalancer --external-ip=$(minikube ip)
    kubectl -n platform expose service/minio --name minio-external --port 9001 --target-port 9001 --type LoadBalancer --external-ip $(minikube ip)
    kubectl -n platform expose service/nginx --name nginx-external --port 443 --target-port 443 --type LoadBalancer --external-ip $(minikube ip)

Create Kafka topics:

    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic events --config "retention.ms=604800000" --replication-factor=1 --partitions=16

    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-0 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-1 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-2 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-3 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-4 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64

    kubectl -n platform exec $(kubectl -n platform get pod -l app=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic batch --config "retention.ms=604800000" --replication-factor=1 --partitions=16

Create Minio bucket:

    kubectl -n platform delete job -l app=minio-init 
    kubectl -n platform apply -f scripts/minio-init.yaml 

Export GitHub secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Create secrets for services:

    kubectl -n services create secret generic authentication --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal GITHUB_ACCOUNT_EMAIL=$GITHUB_ACCOUNT_EMAIL --from-literal GITHUB_CLIENT_ID=$GITHUB_CLIENT_ID --from-literal GITHUB_CLIENT_SECRET=$GITHUB_CLIENT_SECRET

    kubectl -n services create secret generic accounts --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password

    kubectl -n services create secret generic designs-query --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password
    kubectl -n services create secret generic designs-command --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n services create secret generic designs-aggregate --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
    kubectl -n services create secret generic designs-notify --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret 
    kubectl -n services create secret generic designs-render --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password

    kubectl -n services create secret generic gateway --from-file keystore_client.jks=secrets/keystore_client.jks --from-file truststore_client.jks=secrets/truststore_client.jks --from-file keystore_server.jks=secrets/keystore_server.jks --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret

    kubectl -n services create secret generic frontend --from-file ca_cert.pem=secrets/ca_cert.pem --from-file server_cert.pem=secrets/server_cert.pem --from-file server_key.pem=secrets/server_key.pem

Export version:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)

Export logging level:

    export LOGGING_LEVEL=DEBUG

Deploy services:

    helm install service-authentication services/authentication/helm -n services --set image.repository=integration/authentication,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):443,clientAuthUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm install service-accounts services/accounts/helm -n services --set image.repository=integration/accounts,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm install service-designs-query services/designs-query/helm -n services --set image.repository=integration/designs-query,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm install service-designs-command services/designs-command/helm -n services --set image.repository=integration/designs-command,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm install service-designs-aggregate services/designs-aggregate/helm -n services --set image.repository=integration/designs-aggregate,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm install service-designs-notify services/designs-notify/helm -n services --set image.repository=integration/designs-notify,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm install service-designs-render services/designs-render/helm -n services --set image.repository=integration/designs-render,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm install service-gateway services/gateway/helm -n services --set image.repository=integration/gateway,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm install service-frontend services/frontend/helm -n services --set image.repository=integration/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=https://$(minikube ip):443,clientApiUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}

Check services:

    kubectl -n services logs -f --tail=-1 -l app=authentication
    kubectl -n services logs -f --tail=-1 -l app=accounts
    kubectl -n services logs -f --tail=-1 -l app=designs-query
    kubectl -n services logs -f --tail=-1 -l app=designs-command
    kubectl -n services logs -f --tail=-1 -l app=designs-aggregate
    kubectl -n services logs -f --tail=-1 -l app=designs-notify
    kubectl -n services logs -f --tail=-1 -l app=designs-render
    kubectl -n services logs -f --tail=-1 -l app=gateway
    kubectl -n services logs -f --tail=-1 -l app=frontend

Expose services:

    kubectl -n services expose service/designs-notify --name designs-notify-external --port 8000 --target-port 8080 --type LoadBalancer --external-ip $(minikube ip)

Scale services:

    kubectl -n services scale deployment authentication --replicas=2
    kubectl -n services scale deployment accounts --replicas=2
    kubectl -n services scale deployment designs-query --replicas=2
    kubectl -n services scale deployment designs-command --replicas=2
    kubectl -n services scale deployment designs-aggregate --replicas=2
    kubectl -n services scale deployment designs-notify --replicas=1
    kubectl -n services scale deployment designs-render --replicas=8
    kubectl -n services scale deployment gateway --replicas=2
    kubectl -n services scale deployment frontend --replicas=2

Scale platform:

    kubectl -n services scale deployment nginx --replicas=2

Open browser:

    open https://$(minikube ip)/browse/designs.html

Login with your GitHub account associated with the admin email for getting admin access

Upgrade services (if needed):

    helm upgrade --install service-authentication services/authentication/helm -n services --set image.repository=integration/authentication,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):443,clientAuthUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm upgrade --install service-accounts services/accounts/helm -n services --set image.repository=integration/accounts,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm upgrade --install service-designs-query services/designs-query/helm -n services --set image.repository=integration/designs-query,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm upgrade --install service-designs-command services/designs-command/helm -n services --set image.repository=integration/designs-command,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm upgrade --install service-designs-aggregate services/designs-aggregate/helm -n services --set image.repository=integration/designs-aggregate,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm upgrade --install service-designs-notify services/designs-notify/helm -n services --set image.repository=integration/designs-notify,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
    helm upgrade --install service-designs-render services/designs-render/helm -n services --set image.repository=integration/designs-render,image.tag=${VERSION},replicas=4,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm upgrade --install service-gateway services/gateway/helm -n services --set image.repository=integration/gateway,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

    helm upgrade --install service-frontend services/frontend/helm -n services --set image.repository=integration/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=https://$(minikube ip):443,clientApiUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}

Upgrade Elasticsearch (if needed):

    helm upgrade --install integration-elasticsearch helm/elasticsearch -n monitoring --set replicas=1,volumeSize=20Gi

Upgrade Kibana (if needed):

    helm upgrade --install integration-kibana helm/kibana -n monitoring --set replicas=1,server.publicBaseUrl=http://$(minikube ip)::5601

Upgrade Jaeger (if needed):

    helm upgrade --install integration-jaeger helm/jaeger -n monitoring --set replicas=1

Upgrade Elasticsearch (if needed):

    helm upgrade --install integration-elasticsearch helm/elasticsearch -n platform --set replicas=1

Upgrade Cassandra (if needed):

    helm upgrade --install integration-cassandra helm/cassandra -n platform --set replicas=1

Upgrade Zookeeper (if needed):

    helm upgrade --install integration-zookeeper helm/zookeeper -n platform --set replicas=1

Upgrade Kafka (if needed):

    helm upgrade --install integration-kafka helm/kafka -n platform --set replicas=1,externalName=$(minikube ip):9093

Upgrade MySQL (if needed):

    helm upgrade --install integration-mysql helm/mysql -n platform --set replicas=1

Upgrade Consul (if needed):

    helm upgrade --install integration-consul helm/consul -n platform --set replicas=1,servicePort=8000,serviceName=$(minikube ip)

Upgrade Minio (if needed):

    helm upgrade --install integration-minio helm/minio -n platform --set replicas=1

Upgrade NGINX (if needed):

    helm upgrade --install integration-nginx helm/nginx -n platform --set replicas=1,hostname=$(minikube ip)

Stop Minikube (when finished):

    minikube stop

Delete Minikube (when finished):

    minikube delete




