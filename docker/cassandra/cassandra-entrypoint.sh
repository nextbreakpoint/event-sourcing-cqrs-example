#!/bin/sh
exec /cassandra-initdb.sh &
exec /usr/local/bin/docker-entrypoint.sh "$@"
