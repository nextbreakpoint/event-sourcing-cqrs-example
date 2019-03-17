#!/bin/sh

./swarm/deploy_stack.sh shop-mysql
./swarm/deploy_stack.sh shop-auth
./swarm/deploy_stack.sh shop-accounts
./swarm/deploy_stack.sh shop-designs
./swarm/deploy_stack.sh shop-web
./swarm/deploy_stack.sh shop-gateway
./swarm/deploy_stack.sh shop-nginx
./swarm/deploy_stack.sh shop-agents
