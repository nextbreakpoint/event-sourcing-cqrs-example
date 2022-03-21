#!/bin/sh
/usr/local/bin/docker-entrypoint.sh | tee /usr/share/elasticsearch/logs/console.log
