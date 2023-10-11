#!/bin/bash

set -e

pipeline/check-nexus.sh

if [ $? -eq 0 ]

then

pipeline/create-repository.sh

else

docker logs nexus

fi