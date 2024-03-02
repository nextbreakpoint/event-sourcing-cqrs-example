#!/bin/bash

set -e

export REPOSITORY="integration"
export VERSION=""

POSITIONAL_ARGS=()

for i in "$@"; do
  case $i in
    --version=*)
      VERSION="${i#*=}"
      shift
      ;;
    --docker-repository=*)
      REPOSITORY="${i#*=}"
      shift
      ;;
    -*)
      echo "Unknown option $i"
      exit 1
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ -z $VERSION ]]; then
  echo "Missing or invalid value for argument: --version"
  exit 1
fi

if [[ -z $REPOSITORY ]]; then
  echo "Missing or invalid value for argument: --docker-repository"
  exit 1
fi

services=(
  designs-query
  designs-command
  designs-aggregate
  designs-watch
  designs-render
  accounts
  authentication
  frontend
)

for service in "${services[@]}"; do
  echo "load image: ${REPOSITORY}/$service:${VERSION}"
  docker tag "${REPOSITORY}/$service:${VERSION}" "localhost:5000/${REPOSITORY}/$service:${VERSION}"
  docker push "localhost:5000/${REPOSITORY}/$service:${VERSION}"
done
