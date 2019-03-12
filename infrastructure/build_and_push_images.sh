#!/bin/sh

pushd $(pwd)/../services

./scripts/deploy.sh $1 $2 $3

popd
