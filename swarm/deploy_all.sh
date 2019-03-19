#!/bin/sh

./swarm/deploy_stack.sh shop-mysql
./swarm/deploy_stack.sh shop-authentication
./swarm/deploy_stack.sh shop-accounts
./swarm/deploy_stack.sh shop-designs
./swarm/deploy_stack.sh shop-weblets
./swarm/deploy_stack.sh shop-gateway
./swarm/deploy_stack.sh shop-nginx
./swarm/deploy_stack.sh shop-agents
