#!/bin/bash

set -e

VERSION=""
LOGGING_LEVEL="INFO"

GITHUB_ACCOUNT_EMAIL=""
GITHUB_CLIENT_ID=""
GITHUB_CLIENT_SECRET=""

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
    --debug)
      LOGGING_LEVEL="DEBUG"
      shift
      ;;
    --version=*)
      VERSION="${i#*=}"
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

export LOGGING_LEVEL
export VERSION
export GITHUB_ACCOUNT_EMAIL
export GITHUB_CLIENT_ID
export GITHUB_CLIENT_SECRET

case $COMMAND in
  start)
    if [[ -z $VERSION ]]; then
      echo "Missing or invalid value for argument: --version"
      exit 1
    fi

    if [[ -z $GITHUB_ACCOUNT_EMAIL ]]; then
      echo "Missing variable: GITHUB_ACCOUNT_EMAIL"
      exit 1
    fi

    if [[ -z $GITHUB_CLIENT_ID ]]; then
      echo "Missing variable: GITHUB_CLIENT_ID"
      exit 1
    fi

    if [[ -z $GITHUB_CLIENT_SECRET ]]; then
      echo "Missing variable: GITHUB_CLIENT_SECRET"
      exit 1
    fi

    docker compose -f docker-compose-services.yaml -p services up -d --wait
    ;;
  stop)
    docker compose -f docker-compose-services.yaml -p services down
    ;;
  destroy)
    docker compose -f docker-compose-services.yaml -p services down --volumes
    ;;
  *)
esac
