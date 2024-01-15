#!/bin/bash

set +e

docker run -i --network platform_bridge -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password --entrypoint sh minio/mc:latest < scripts/minio-create-bucket.sh