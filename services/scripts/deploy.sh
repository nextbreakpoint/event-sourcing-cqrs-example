#!/bin/sh

pushd common

mvn clean install

popd

mvn clean install

mvn docker:build docker:push -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
