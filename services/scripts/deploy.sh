#!/bin/sh

pushd common

#mvn clean install

popd

#mvn clean install

mvn io.fabric8:docker-maven-plugin:push@push -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
