#!/bin/bash

set -e

images=(
  elasticsearch
  cassandra
  zookeeper
  kafka
)

for image in ${images[@]}; do
  pushd docker/$image
   sh ./build.sh
  popd
done
