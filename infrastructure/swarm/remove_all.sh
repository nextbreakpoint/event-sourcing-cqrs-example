#!/bin/sh

./swarm/remove_stack.sh shop-agents
./swarm/remove_stack.sh shop-nginx
./swarm/remove_stack.sh shop-gateway
./swarm/remove_stack.sh shop-web
./swarm/remove_stack.sh shop-designs
./swarm/remove_stack.sh shop-accounts
./swarm/remove_stack.sh shop-auth
./swarm/remove_stack.sh shop-mysql
