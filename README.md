# event-sourcing-cqrs-example

This project is an example of web application using event-driven services and distributed data stores.
The frontend is Node.js with Express, Handlebars, and React. The backend is Java with Vertx, Kafka, Cassandra, Elasticsearch, and Minio.
The application runs on Kubernetes. Helm charts for deploying the application on Kubernetes are provided and have been tested on Minikube.
The application can also run on Docker. Docker compose files for running and debugging the application are provided.

## Preparation

There are few certificates which are required to run the application. For the purpose of this example we use self-signed certificates.

Generate secrets:

    ./scripts/secrets.sh

Add self-signed CA certificate to trusted certificates (for Mac only):

    security -v add-trusted-cert -r trustRoot -k ~/Library/Keychains/login.keychain secrets/ca_cert.pem

## Build and run on Docker

Docker is the recommended environment for developing and testing the services.

### Preparation

Build Docker images required for running platform and testing services:

    ./scripts/build-images.sh

### Build services

Start Nexus and Pact Broker:

    ./scripts/docker-pipeline.sh --start

Wait until Nexus is ready (please be patient):

    ./scripts/wait-for.sh --timeout=60 --command="docker logs --tail=100 nexus | grep 'Started Sonatype Nexus'"

Export Nexus password:

    export NEXUS_PASSWORD=$(docker exec nexus cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    ./scripts/create-repository.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD}

Build services:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD}

See results of Pact tests:

    open http://localhost:9292

Build services without tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests

Build services without tests and set version:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests --version=1.0.0

Build services without tests but keep version:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests --keep-version

Build only two services without tests and keep version:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-tests --keep-version --services="frontend authentication"

Build services and run tests, but skip Pact tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-pact-tests --skip-pact-verify

Build services and run tests, but skip integration tests:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-integration-tests

Run tests without building Docker images:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --keep-version

Run Pact tests only:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --keep-version --skip-integration-tests

Run Pact verify only:

    ./scripts/build-services.sh --nexus-host=localhost --nexus-port=8082 --nexus-username=admin --nexus-password=${NEXUS_PASSWORD} --skip-images --keep-version --skip-integration-tests --skip-pact-tests

Update dependencies (only if you know what you are doing):

    ./scripts/update-dependencies.sh

Tail pipeline containers:

    docker logs -f --tail=-1 nexus
    docker logs -f --tail=-1 postgres
    docker logs -f --tail=-1 pact-server

### Run platform and services 

Start platform:

    ./scripts/docker-platform.sh --start

Wait until Kafka is ready:

    ./scripts/wait-for.sh --timeout=60 --command="docker logs --tail=-1 kafka | grep 'started (kafka.server.KafkaServer)'"

Create Kafka topics:

    ./scripts/docker-create-topics.sh

Create Minio bucket:

    docker run -i --network platform_bridge -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest < scripts/minio-create-bucket.sh

Create a GitHub OAuth application like:

    Homepage URL: 
    http://localhost:8000

    Authorization Callback URL:
    http://localhost:8000/v1/auth/callback

then export the application secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Start services:

    ./scripts/docker-services.sh --start --version=$(./scripts/get-version.sh)

Open application:

    open http://localhost:8000/browse/designs.html

Follow login link and log into the GitHub account when asked in order to access the administration console.

Open Minio console:

    open http://localhost:9091

Login with user 'admin' and password 'password'.

Open Jaeger console:

    open http://localhost:16686

### Troubleshooting 

Restart services with debug logging level:

    ./scripts/docker-services.sh --start --version=$(./scripts/get-version.sh) --debug

Tail services containers:

    docker logs -f --tail=-1 authentication
    docker logs -f --tail=-1 accounts
    docker logs -f --tail=-1 designs-query
    docker logs -f --tail=-1 designs-command
    docker logs -f --tail=-1 designs-aggregate
    docker logs -f --tail=-1 designs-watch
    docker logs -f --tail=-1 designs-render1
    docker logs -f --tail=-1 designs-render2
    docker logs -f --tail=-1 designs-render3
    docker logs -f --tail=-1 designs-render4
    docker logs -f --tail=-1 frontend

Tail platform containers:

    docker logs -f --tail=-1 kafka
    docker logs -f --tail=-1 zookeeper
    docker logs -f --tail=-1 elasticsearch
    docker logs -f --tail=-1 cassandra
    docker logs -f --tail=-1 minio
    docker logs -f --tail=-1 mysql
    docker logs -f --tail=-1 nginx
    docker logs -f --tail=-1 jaeger

### Cleanup

Stop services:

    ./scripts/docker-services.sh --stop

Stop platform:

    ./scripts/docker-platform.sh --stop

Remove platform volumes:

    ./scripts/docker-platform.sh --destroy

Stop pipeline:

    ./scripts/docker-pipeline.sh --stop

Remove pipeline volumes:

    ./scripts/docker-pipeline.sh --destroy

Remove Docker images:

    docker image rm -f $(docker image ls 'integration/*' -q)
    docker image rm $(docker image ls -f dangling=true -q)

## Deploy on Minikube

Minikube is the recommended environment for testing the deployment of the services. 

### Preparation

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
    minikube addons enable ingress

Load Docker images required for running platform:

    ./scripts/minikube-load-platform-images.sh 

Load Docker images required for running services:

    ./scripts/minikube-load-services-images.sh --version=$(./scripts/get-version.sh)

### Run platform and services

Create a GitHub OAuth application like:

    Homepage URL: 
    https://<minikube-ip>

    Authorization Callback URL:
    https://<minikube-ip>/v1/auth/callback

where minikube-ip is the address of Minikube:

    minikube ip

then export the application secrets:

    export GITHUB_ACCOUNT_EMAIL=your-account-id
    export GITHUB_CLIENT_ID=your-client-id
    export GITHUB_CLIENT_SECRET=your-client-secret

Create secrets:

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

Create Kibana index pattern:

    curl "http://$(minikube ip):5601/api/index_patterns/index_pattern" -H "kbn-xsrf: reporting" -H "Content-Type: application/json" -d @$(pwd)/scripts/index-pattern.json

Deploy services:

    ./scripts/helm-install-services.sh --version=$(./scripts/get-version.sh)

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

Open application:

    open https://$(minikube ip)/browse/designs.html

Follow login link and log into the GitHub account when asked in order to access the administration console.

Open Jaeger console:

    open http://$(minikube ip):16686

Open Kibana console:

    open http://$(minikube ip):5601

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

### Troubleshooting

Redeploy services with debug logging level:

    ./scripts/helm-install-services.sh --version=$(./scripts/get-version.sh) --debug

Tail services containers:

    kubectl -n services logs -f --tail=-1 -l component=authentication
    kubectl -n services logs -f --tail=-1 -l component=accounts
    kubectl -n services logs -f --tail=-1 -l component=designs-query
    kubectl -n services logs -f --tail=-1 -l component=designs-command
    kubectl -n services logs -f --tail=-1 -l component=designs-aggregate
    kubectl -n services logs -f --tail=-1 -l component=designs-watch
    kubectl -n services logs -f --tail=-1 -l component=designs-render
    kubectl -n services logs -f --tail=-1 -l component=frontend

Tail platform containers:

    kubectl -n platform logs -f --tail=-1 -l component=kafka
    kubectl -n platform logs -f --tail=-1 -l component=zookeeper
    kubectl -n platform logs -f --tail=-1 -l component=elastichsearch
    kubectl -n platform logs -f --tail=-1 -l component=cassandra
    kubectl -n platform logs -f --tail=-1 -l component=minio
    kubectl -n platform logs -f --tail=-1 -l component=mysql
    kubectl -n platform logs -f --tail=-1 -l component=nginx

### Cleanup

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

### Build services (optional)

Although it is not recommended, it is possible to use Minikube to run the pipeline required to build the services.  

#### Use Docker engine running on Minikube

Deploy Nexus and Pact Broker:

    ./scripts/helm-install-pipeline.sh

Wait until Nexus is ready:

    ./scripts/wait-for.sh --timeout=60 --command="kubectl -n pipeline logs --tail=100 -l component=nexus | grep 'Started Sonatype Nexus'"

Expose Nexus and Pact Broker:

    ./scripts/kube-expose-pipeline.sh

Export variables:

    export NEXUS_HOST=$(minikube ip)
    export NEXUS_PORT=8081
    export NEXUS_USERNAME=admin
    export NEXUS_PASSWORD=$(kubectl -n pipeline exec $(kubectl -n pipeline get pod -l component=nexus -o json | jq -r '.items[0].metadata.name') -c nexus -- cat /opt/sonatype/sonatype-work/nexus3/admin.password)

Create Maven repository:

    ./scripts/create-repository.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

Select Docker engine running on Minikube:

    eval $(minikube docker-env)

Build Docker images (and skip tests):

    ./scripts/build-services.sh --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD} --docker-host=$(minikube ip) --skip-tests

Please note that integration tests or pact tests are not supported when using the Docker engine running on Minikube.

#### Use Docker engine running on Host 

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

Reset Docker environment variables to use local Docker engine:

    eval $(env | grep DOCKER_ | cut -f 1 -d "=" - | awk '{print "unset "$1}')

Build images and run tests:

    ./scripts/build-services.sh --pactbroker-host=${PACTBROKER_HOST} --pactbroker-port=${PACTBROKER_PORT} --nexus-host=${NEXUS_HOST} --nexus-port=${NEXUS_PORT} --nexus-username=${NEXUS_USERNAME} --nexus-password=${NEXUS_PASSWORD}

See results of Pact tests:

    open http://$(minikube ip):9292

Load Docker images into Minikube:

    ./scripts/minikube-load-images.sh --version=$(./scripts/get-version.sh)
