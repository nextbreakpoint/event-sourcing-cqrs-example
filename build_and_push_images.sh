#!/bin/sh

pushd $(pwd)/platform

./scripts/secrets.sh

./scripts/deploy.sh $1 $2 $3

popd
