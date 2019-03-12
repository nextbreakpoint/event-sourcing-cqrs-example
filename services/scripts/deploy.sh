#!/bin/sh

set -e

pushd common

mvn clean install

popd

mvn clean compile -T 4 -DskipTests=true

mvn package io.fabric8:docker-maven-plugin:push@push -DskipTests=true -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
