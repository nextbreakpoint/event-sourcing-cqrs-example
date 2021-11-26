#!/bin/sh

set -e

export REPOSITORY=${1:-integration}
export VERSION=${2:-1.0.0}
export BUILD="true"
export TEST="false"

export NEXUS_HOST=$(minikube ip)
export NEXUS_PORT="8081"
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=password

#mvn clean deploy -s settings.xml -Dnexus=true
#
#pushd common
#mvn clean deploy -s settings.xml -Dnexus=true
#popd
#
#pushd services
#mvn clean deploy -s settings.xml -Dnexus=true
#popd

export JAEGER_SERVICE_NAME=integration
#
#pushd services/frontend
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/gateway
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/authentication
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
pushd services/accounts
if [ "$BUILD" == "true" ]; then
 sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
fi
if [ "$TEST" == "true" ]; then
 mvn clean verify -Dminikube=true
fi
popd
#
#pushd services/designs-notification-dispatcher
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/designs-command-producer
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/designs-aggregate-fetcher
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/designs-event-consumer
#if [ "$BUILD" == "true" ]; then
# sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
# mvn clean verify -Dminikube=true
#fi
#popd
#
#pushd services/designs-tile-renderer
#if [ "$BUILD" == "true" ]; then
#  sh build.sh $REPOSITORY $VERSION ${NEXUS_HOST} ${NEXUS_PORT}
#fi
#if [ "$TEST" == "true" ]; then
#  mvn clean verify -Dminikube=true
#fi
#popd
