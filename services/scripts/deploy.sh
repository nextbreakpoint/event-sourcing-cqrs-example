#!/bin/sh

mvn -pl common clean install && mvn clean install && mvn docker:build docker:push -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
