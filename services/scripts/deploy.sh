#!/bin/sh

set -e

pushd common

mvn clean install

popd

mvn clean verify

mvn docker:build docker:push -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
