#!/bin/sh

$(aws ecr get-login --no-include-email --region eu-west-1)

docker stack deploy -c $SWARM_RESOURCES_PATH/stack-$1.yaml $1 --with-registry-auth
