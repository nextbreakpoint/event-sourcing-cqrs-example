#!/bin/bash

set -e

COMMAND=""

POSITIONAL_ARGS=()

for i in "$@"; do
  case $i in
    --start)
      COMMAND="start"
      shift
      ;;
    --stop)
      COMMAND="stop"
      shift
      ;;
    --destroy)
      COMMAND="destroy"
      shift
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ -z $COMMAND ]]; then
  echo "Missing command: --start|stop|destroy"
  exit 1
fi

case $COMMAND in
  start)
    docker compose -f docker-compose-pipeline.yaml -p pipeline up -d --wait
    ;;
  stop)
    docker compose -f docker-compose-pipeline.yaml -p pipeline down
    ;;
  destroy)
    docker compose -f docker-compose-pipeline.yaml -p pipeline down --volumes
    ;;
  *)
esac