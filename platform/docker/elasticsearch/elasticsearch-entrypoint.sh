#!/bin/sh
cd /
exec ./elasticsearch-initdb.sh &
cd /usr/share/elasticsearch/bin/
/usr/local/bin/docker-entrypoint.sh | tee /usr/share/elasticsearch/logs/console.log
