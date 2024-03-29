#!/bin/bash

set -e

POSITIONAL_ARGS=()

TIMEOUT=10
COMMAND=""

for i in "$@"; do
  case $i in
    --timeout=*)
      TIMEOUT="${i#*=}"
      shift
      ;;
    --command=*)
      COMMAND="${i#*=}"
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

if [[ -z $TIMEOUT ]]; then
  echo "Missing or invalid value for argument: --timeout"
  exit 1
fi

if [[ -z $COMMAND ]]; then
  echo "Missing or invalid value for argument: --command"
  exit 1
fi

timeout=$TIMEOUT

until [ $timeout -le 0 ] || (bash -c "$COMMAND" &> /dev/null); do
    echo "waiting for: $COMMAND"
    sleep 1
    timeout=$((timeout - 1))
done

if [ $timeout -le 0 ]; then
    echo "giving up"
    exit 1
fi

exit 0
