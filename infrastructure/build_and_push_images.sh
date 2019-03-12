#!/bin/sh

pushd $(pwd)/../services

./deploy.sh $1 $2 $3

popd
