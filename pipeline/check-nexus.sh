#!/bin/bash

set -e

declare -i n=1

delay=5
attempts=60

until bash -c 'docker exec nexus cat /opt/sonatype/sonatype-work/nexus3/admin.password'; do echo "Waiting for Nexus..."; sleep $delay; n=$n+1; if [ $n -eq $attempts ]; then exit 1; fi; done

echo "Nexus ready"

exit 0