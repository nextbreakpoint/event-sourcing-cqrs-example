#!/bin/sh

unset SCHEMA_REGISTRY_HOST
unset SCHEMA_REGISTRY_PORT
exec "$@"
