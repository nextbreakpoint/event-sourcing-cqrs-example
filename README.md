# event-sourcing-cqrs-example

This project is an example of web application using event-driven services and distributed data stores. 
The frontend is Node.js with Express, Handlebars, and React. The backend is Java with Vertx, Kafka, Cassandra, Elasticsearch, and Minio.
The application runs on Kubernetes. Helm charts for deploying the entire stack on Minikube for testing the application are provided.
The application can also run on Docker. Docker compose files for running and debugging the application are provided. 

## Preparation

There are few certificates which are required to run the application. For the purpose of this example we use self-signed certificates. 

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

Update dependencies (be careful when changing the dependencies versions):

    mvn versions:update-properties -Dcommon=true -Dservices=true -Dplatform=true
    mvn versions:commit

## Run on Docker

Build Docker images:

    ./scripts/build-images.sh 

Start platform:

    docker compose -f docker-compose-platform.yaml -p platform up -d

Create Kafka topics:

    ./scripts/docker-create-topics.sh

Create Minio bucket:

    docker run -i --network platform_bridge -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest < scripts/minio-init.sh

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

Export version:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)

Export logging level:

    export LOGGING_LEVEL=DEBUG

Start services:

    docker compose -f docker-compose-services.yaml -p services up -d

Open browser:

    open https://localhost:8080/browse/designs.html

Stop services (when finished):

    docker compose -f docker-compose-services.yaml -p services down

Stop platform (when finished):

    docker compose -f docker-compose-platform.yaml -p platform down

## Prepare Minikube

Setup Minikube:

    minikube start --vm-driver=hyperkit --cpus 8 --memory 32768m --disk-size 64g --kubernetes-version v1.22.2

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

## Build on Minikube

Deploy Nexus and Pact Broker:

    ./scripts/helm-install-pipeline.sh

It might take quite a while before Nexus is ready.

Expose Nexus and Pact Broker:

    ./scripts/kube-expose-pipeline.sh

Export variables:

    export PACTBROKER_HOST=$(minikube ip)
    export PACTBROKER_PORT=9092
    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n pipeline exec $(kubectl -n pipeline get pod -l component=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    ./scripts/create-repository.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

Configure Docker:

    eval $(minikube docker-env)

Build services:

    ./scripts/build-services.sh --pactbroker-host=${PACTBROKER_HOST} --pactbroker-port=${PACTBROKER_PORT} --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD} --docker-host=$(minikube ip) --skip-tests

## Run on Minikube

Deploy secrets:

    ./scripts/kube-create-secrets.sh

Deploy Certificate Manager:

    helm repo add jetstack https://charts.jetstack.io
    helm repo update
    helm install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace --version v1.7.1 --set installCRDs=true

Deploy Prometheus operator:

    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo update
    helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring -f scripts/prometheus-values.yaml

Deploy Jaeger operator:

    helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
    helm repo update
    helm install jaeger-operator jaegertracing/jaeger-operator --namespace monitoring --set rbac.create=true

Deploy Fluent Bit:

    helm repo add fluent https://fluent.github.io/helm-charts
    helm repo update
    helm upgrade --install fluent-bit fluent/fluent-bit -n monitoring -f scripts/fluentbit-values.yaml

Configure Docker:

    eval $(minikube docker-env)

Build Docker images:

    ./scripts/build-images.sh 

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

Export GitHub secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Export version:

    export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)

Export logging level:

    export LOGGING_LEVEL=DEBUG

Deploy services:

    ./scripts/helm-install-services.sh

Expose services:

    ./scripts/kube-expose-services.sh

Create monitoring resources:

    kubectl apply -f scripts/jaeger.yaml
    kubectl apply -f scripts/services-monitoring.yaml
    kubectl apply -f scripts/grafana-datasource.yaml
    kubectl apply -f scripts/grafana-dashboards.yaml

Scale services:

    kubectl -n services scale deployment designs-query --replicas=2
    kubectl -n services scale deployment designs-aggregate --replicas=2
    kubectl -n services scale deployment designs-notify --replicas=1
    kubectl -n services scale deployment designs-render --replicas=4
    kubectl -n services scale deployment gateway --replicas=2
    kubectl -n services scale deployment frontend --replicas=2

Scale platform:

    kubectl -n platform scale deployment nginx --replicas=2

Create Kibana index pattern:

    curl "http://$(minikube ip):5601/api/index_patterns/index_pattern" -H "kbn-xsrf: reporting" -H "Content-Type: application/json" -d @$(pwd)/scripts/index-pattern.json

Open browser:

    open https://$(minikube ip)/browse/designs.html

Login with your GitHub account associated with the admin email for getting admin access

## Troubleshooting

Tail services:

    kubectl -n services logs -f --tail=-1 -l component=authentication
    kubectl -n services logs -f --tail=-1 -l component=accounts
    kubectl -n services logs -f --tail=-1 -l component=designs-query
    kubectl -n services logs -f --tail=-1 -l component=designs-command
    kubectl -n services logs -f --tail=-1 -l component=designs-aggregate
    kubectl -n services logs -f --tail=-1 -l component=designs-notify
    kubectl -n services logs -f --tail=-1 -l component=designs-render
    kubectl -n services logs -f --tail=-1 -l component=gateway
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

## Cleanup

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

Delete Minikube:

    minikube delete
