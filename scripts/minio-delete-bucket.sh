#!/bin/sh

set -e

mc alias set minio http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"
mc rb --force minio/fractals || true
