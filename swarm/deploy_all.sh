#!/bin/sh

./swarm/deploy_stack.sh blueprint-mysql
./swarm/deploy_stack.sh blueprint-authentication
./swarm/deploy_stack.sh blueprint-accounts
./swarm/deploy_stack.sh blueprint-designs
./swarm/deploy_stack.sh blueprint-weblets
./swarm/deploy_stack.sh blueprint-gateway
./swarm/deploy_stack.sh blueprint-nginx
./swarm/deploy_stack.sh blueprint-agents
