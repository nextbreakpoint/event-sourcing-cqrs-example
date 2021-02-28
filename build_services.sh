#!/bin/sh
set -e
pushd platform/services/accounts
sh build.sh
popd
pushd platform/services/designs
sh build.sh
popd
pushd platform/services/designs-aggregate-fetcher
sh build.sh
popd
pushd platform/services/designs-command-consumer
sh build.sh
popd
pushd platform/services/designs-command-producer
sh build.sh
popd
pushd platform/services/designs-notification-dispatcher
sh build.sh
popd
pushd platform/services/authentication
sh build.sh
popd
pushd platform/services/gateway
sh build.sh
popd
pushd platform/services/frontend
sh build.sh
popd
