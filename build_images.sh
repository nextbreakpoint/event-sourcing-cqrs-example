#!/bin/sh
set -e
pushd platform/docker/cassandra
sh build.sh
popd
pushd platform/docker/kafka
sh build.sh
popd
pushd platform/docker/zookeeper
sh build.sh
popd
