#!/bin/bash

docker exec $(docker container ls -f name=pipeline-nexus-1 -q) cat /opt/sonatype/sonatype-work/nexus3/admin.password
