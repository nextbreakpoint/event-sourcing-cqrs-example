#!/bin/bash

set -e

REPOSITORY="integration"
VERSION=""

BUILD_SERVICES=""

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
    --services=*)
      BUILD_SERVICES="${i#*=}"
      shift
      ;;
    -*|--*)
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
  export VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
  echo "Selected version: $VERSION"
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

if [[ ! -z $BUILD_SERVICES ]]; then
  services=(
    $BUILD_SERVICES
  )
fi

echo -n "Loading services:"
for service in ${services[@]}; do
  echo -n " "$service
done
echo ""

for service in ${services[@]}; do
  echo "load image: ${REPOSITORY}/$service:${VERSION}"
  minikube image load ${REPOSITORY}/$service:${VERSION}
done
