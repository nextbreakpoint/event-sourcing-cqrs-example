#!/bin/sh

pushd $(pwd)/platform

./scripts/deploy.sh $1 $2 $3

popd
