#!/bin/sh

mvn clean package docker:push -Ddocker.registry=$1 -Ddocker.username=$2 -Ddocker.password=$3
