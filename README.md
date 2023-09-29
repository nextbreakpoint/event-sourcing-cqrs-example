# event-sourcing-cqrs-example

This project is an example of web application using event-driven services and distributed data stores.
The frontend is Node.js with Express, Handlebars, and React. The backend is Java with Vertx, Kafka, Cassandra, Elasticsearch, and Minio.
The application runs on Kubernetes. Helm charts for deploying the entire stack on Minikube for testing the application are provided.
The application can also run on Docker. Docker compose files for running and debugging the application are provided.

## Preparation

There are few certificates which are required to run the application. For the purpose of this example we use self-signed certificates.

Generate secrets:

    ./scripts/secrets.sh

Add self-signed CA certificate to trusted certificates (for Mac only):

    security -v add-trusted-cert -r trustRoot -k ~/Library/Keychains/login.keychain secrets/ca_cert.pem

## Build on Docker

Build Docker images:

    ./scripts/build-images.sh

Start Nexus and Pact Broker:

    docker compose -f docker-compose-pipeline.yaml -p pipeline up -d

Wait until Nexus is ready (please be patient):

    ./scripts/wait-for.sh --timeout=60 --command="docker logs --tail=100 $(docker ps | grep nexus | cut -d ' ' -f 1) | grep 'Started Sonatype Nexus'"

Export Nexus password:

    export NEXUS_PASSWORD=$(docker exec $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    ./scripts/create-repository.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD}

Build services:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD}

See results of Pact tests:

    open http://localhost:9292

Build services without tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests

Build services without tests but keep version:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests --skip-deploy --version=$(./scripts/get-version.sh)

Build only two services without tests and keep version:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests --skip-deploy --version=$(./scripts/get-version.sh) --services="frontend authentication"

Build services and run tests, but skip Pact tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-pact-tests --skip-pact-verify

Build services and run tests, but skip integration tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-integration-tests

Run tests without building Docker images:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --skip-deploy --version=$(./scripts/get-version.sh)

Run Pact tests only:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --skip-deploy --version=$(./scripts/get-version.sh) --skip-integration-tests

Run Pact verify only:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --skip-deploy --version=$(./scripts/get-version.sh) --skip-integration-tests --skip-pact-tests

Update dependencies (only if you know what you are doing):

    ./scripts/update-dependencies.sh

## Run on Docker

Start platform:

    docker compose -f docker-compose-platform.yaml -p platform up -d

Wait until Kafka is ready:

    ./scripts/wait-for.sh --timeout=60 --command="docker logs --tail=-1 $(docker ps | grep kafka | cut -d ' ' -f 1) | grep 'started (kafka.server.KafkaServer)'"

Create Kafka topics:

    ./scripts/docker-create-topics.sh

Create Minio bucket:

    docker run -i --network platform_bridge -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest < scripts/minio-create-bucket.sh

Create GitHub application and export GitHub secrets (very important):

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Export version:

    export VERSION=$(./scripts/get-version.sh)

Export logging level:

    export LOGGING_LEVEL=INFO

Start services:

    docker compose -f docker-compose-services.yaml -p services up -d

Open application:

    open https://localhost:31443/browse/designs.html

Login with your GitHub account associated with the admin email for getting admin access

Open Jaeger console:

    open http://localhost:16686

Open Kibana console:

    open http://localhost:5601

Open Consul console:

    open http://localhost:8500

Open Minio console:

    open http://localhost:9091

Login with user 'admin' and password 'password'.

## Troubleshooting on Docker

Change logging level:

    export LOGGING_LEVEL=DEBUG

Restart services:

    docker compose -f docker-compose-services.yaml -p services up -d

Tail services:

    docker logs -f --tail=-1 $(docker ps | grep authentication | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep accounts | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-query | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-command | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-aggregate | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-watch | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-render1 | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-render2 | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-render3 | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep designs-render4 | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep frontend | cut -d ' ' -f 1)

Tail platform:

    docker logs -f --tail=-1 $(docker ps | grep kafka | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep zookeeper | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep elasticsearch | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep cassandra | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep minio | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep consul | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep mysql | cut -d ' ' -f 1)
    docker logs -f --tail=-1 $(docker ps | grep nginx | cut -d ' ' -f 1)

## Cleanup on Docker

Stop services:

    docker compose -f docker-compose-services.yaml -p services down

Stop platform:

    docker compose -f docker-compose-platform.yaml -p platform down

Remove platform volumes:

    docker compose -f docker-compose-platform.yaml -p platform down --volumes

Stop pipeline:

    docker compose -f docker-compose-pipeline.yaml -p pipeline down

Remove pipeline volumes:

    docker compose -f docker-compose-pipeline.yaml -p pipeline down --volumes

Remove Docker images:

    docker image rm -f $(docker image ls 'integration/*' -q)
    docker image rm $(docker image ls -f dangling=true -q)

## Prepare Minikube

Setup Minikube:

    minikube start --vm-driver=hyperkit --cpus 8 --memory 49152m --disk-size 64g --kubernetes-version=v1.27.6

Create alias (unless you already have kubectl installed):

    alias kubectl="minikube kubectl --"

Create namespaces:

    ./scripts/kube-create-namespaces.sh

Create volumes:

    kubectl apply -f scripts/volumes.yaml

Install addons:

    minikube addons enable metrics-server
    minikube addons enable dashboard
    minikube addons enable registry

Configure Docker:

    eval $(minikube docker-env)

Build Docker images:

    ./scripts/build-images.sh

## Build on Minikube

Deploy Nexus and Pact Broker:

    ./scripts/helm-install-pipeline.sh

Wait until Nexus is ready:

    ./scripts/wait-for.sh --timeout=60 --command="kubectl -n pipeline logs --tail=100 -l component=nexus | grep 'Started Sonatype Nexus'"

Expose Nexus and Pact Broker:

    ./scripts/kube-expose-pipeline.sh

Export variables:

    export PACTBROKER_HOST=$(minikube ip)
    export PACTBROKER_PORT=9292
    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n pipeline exec $(kubectl -n pipeline get pod -l component=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    ./scripts/create-repository.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

Select Docker engine running on Minikube:

    eval $(minikube docker-env)

Build Docker images (and skip tests):

    ./scripts/build-services.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD} --skip-tests

Please note that integration tests or pact tests can't be executed using the Docker engine running on Minikube. 

In case we want to run the tests using Pack Broker and Nexus server deployed on Minikube, we must use the local Docker engine, and then load the Docker images into Minikube.

Reset Docker environment variables to use local Docker engine:

    eval $(env | grep DOCKER_ | cut -f 1 -d "=" - | awk '{print "unset "$1}')

Build images and run tests using local Docker engine:

    ./scripts/build-services.sh --pactbroker-host=${PACTBROKER_HOST} --pactbroker-port=${PACTBROKER_PORT} --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

See results of Pact tests:

    open http://$(minikube ip):9292

Load Docker images into Minikube:

    ./scripts/minikube-load-images.sh --version=$(./scripts/get-version.sh)

## Run on Minikube

Export GitHub secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Deploy secrets:

    ./scripts/kube-create-nginx-secrets.sh
    ./scripts/kube-create-services-secrets.sh

Deploy Certificate Manager:

    helm repo add jetstack https://charts.jetstack.io
    helm repo update
    helm upgrade --install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace --version v1.7.1 --set installCRDs=true

Deploy Prometheus operator:

    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo update
    helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring -f scripts/prometheus-values.yaml

Deploy Jaeger operator:

    helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
    helm repo update
    helm upgrade --install jaeger-operator jaegertracing/jaeger-operator --namespace monitoring --set rbac.create=true --version 2.47.0

Deploy Fluent Bit:

    helm repo add fluent https://fluent.github.io/helm-charts
    helm repo update
    helm upgrade --install fluent-bit fluent/fluent-bit -n monitoring -f scripts/fluentbit-values.yaml

Configure Docker (very important):

    eval $(minikube docker-env)

Deploy monitoring:

    ./scripts/helm-install-monitoring.sh

Expose monitoring:

    ./scripts/kube-expose-monitoring.sh

Deploy platform:

    ./scripts/helm-install-platform.sh

Expose platform:

    ./scripts/kube-expose-platform.sh

Create Kafka topics:

    ./scripts/kube-create-topics.sh

Create Cassandra tables:

    kubectl -n platform exec $(kubectl -n platform get pod -l component=cassandra -o json | jq -r '.items[0].metadata.name') -- cqlsh -u cassandra -p cassandra < scripts/init.cql  

Create Elasticsearch index:

    kubectl -n platform exec $(kubectl -n platform get pod -l component=elasticsearch -o json | jq -r '.items[0].metadata.name') -- sh -c "$(cat scripts/init.sh)"

Create Minio bucket:

    kubectl -n platform delete job -l component=minio-init
    kubectl -n platform apply -f scripts/minio-init.yaml

Export version:

    export VERSION=$(./scripts/get-version.sh)

Export logging level:

    export LOGGING_LEVEL=INFO

Deploy services:

    ./scripts/helm-install-services.sh

Create monitoring resources:

    kubectl apply -f scripts/jaeger.yaml
    kubectl apply -f scripts/services-monitoring.yaml
    kubectl apply -f scripts/grafana-datasource.yaml
    kubectl apply -f scripts/grafana-dashboards.yaml

Scale services:

    kubectl -n services scale deployment designs-query --replicas=2
    kubectl -n services scale deployment designs-aggregate --replicas=2
    kubectl -n services scale deployment designs-watch --replicas=2
    kubectl -n services scale deployment designs-render --replicas=4
    kubectl -n services scale deployment frontend --replicas=2

Scale platform:

    kubectl -n platform scale deployment nginx --replicas=2

Configure autoscaling:

    kubectl -n services apply -f scripts/services-autoscaling.yaml

Create Kibana index pattern:

    curl "http://$(minikube ip):5601/api/index_patterns/index_pattern" -H "kbn-xsrf: reporting" -H "Content-Type: application/json" -d @$(pwd)/scripts/index-pattern.json

Open application:

    open https://$(minikube ip)/browse/designs.html

Login with your GitHub account associated with the admin email for getting admin access

Open Jaeger console:

    open http://$(minikube ip):16686

Open Kibana console:

    open http://$(minikube ip):5601

Open Consul console:

    open http://$(minikube ip):8500

Open Prometheus console:

    open http://$(minikube ip):9090

Open Grafana console:

    open http://$(minikube ip):3000

Login with user 'admin' and password 'password'.

Open Minio console:

    open http://$(minikube ip):9001

Login with user 'admin' and password 'password'.

Open Minikube dashboard:

    minikube dashboard

## Troubleshooting on Minikube

Change logging level:

    export LOGGING_LEVEL=DEBUG

Redeploy services:

    ./scripts/helm-install-services.sh

Tail services:

    kubectl -n services logs -f --tail=-1 -l component=authentication
    kubectl -n services logs -f --tail=-1 -l component=accounts
    kubectl -n services logs -f --tail=-1 -l component=designs-query
    kubectl -n services logs -f --tail=-1 -l component=designs-command
    kubectl -n services logs -f --tail=-1 -l component=designs-aggregate
    kubectl -n services logs -f --tail=-1 -l component=designs-watch
    kubectl -n services logs -f --tail=-1 -l component=designs-render
    kubectl -n services logs -f --tail=-1 -l component=frontend

Tail platform:

    kubectl -n platform logs -f --tail=-1 -l component=kafka
    kubectl -n platform logs -f --tail=-1 -l component=zookeeper
    kubectl -n platform logs -f --tail=-1 -l component=elastichsearch
    kubectl -n platform logs -f --tail=-1 -l component=cassandra
    kubectl -n platform logs -f --tail=-1 -l component=minio
    kubectl -n platform logs -f --tail=-1 -l component=consul
    kubectl -n platform logs -f --tail=-1 -l component=mysql
    kubectl -n platform logs -f --tail=-1 -l component=nginx

## Cleanup on Minikube

Uninstall services:

    ./scripts/helm-uninstall-services.sh

Uninstall platform:

    ./scripts/helm-uninstall-platform.sh

Uninstall monitoring:

    ./scripts/helm-uninstall-monitoring.sh

Uninstall Nexus and Pact Broker:

    ./scripts/helm-uninstall-pipeline.sh

Stop Minikube:

    minikube stop

Delete Minikube (all data saved in hostpath volumes will be lost):

    minikube delete
