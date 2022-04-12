#!/bin/bash

set -e

echo -n $DOCKER_PASSWORD | docker login --username=$DOCKER_USERNAME --password-stdin
