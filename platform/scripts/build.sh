#!/bin/sh

set -e

eval $(minikube docker-env)

export REPOSITORY=integration

# mvn clean install

# pushd common
# mvn clean install
# popd

pushd services/frontend
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/gateway
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/authentication
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/accounts
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/designs
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/designs-command-consumer
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/designs-command-producer
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/designs-notification-dispatcher
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd

pushd services/designs-aggregate-fetcher
sh build.sh $REPOSITORY
mvn clean verify -Dintegration=true
popd
