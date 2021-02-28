#!/bin/sh

./swarm/remove_stack.sh blueprint-agents
./swarm/remove_stack.sh blueprint-nginx
./swarm/remove_stack.sh blueprint-gateway
./swarm/remove_stack.sh blueprint-weblets
./swarm/remove_stack.sh blueprint-designs
./swarm/remove_stack.sh blueprint-accounts
./swarm/remove_stack.sh blueprint-authentication
./swarm/remove_stack.sh blueprint-mysql
