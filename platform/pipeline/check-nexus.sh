#!/bin/bash

declare -i n=1

delay=5
attempts=30

until bash -c 'docker exec -it $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password >> /dev/null'; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done

echo "Nexus ready"

exit 0