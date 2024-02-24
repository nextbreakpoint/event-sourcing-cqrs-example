#!/bin/bash

set -e

REPOSITORY=integration
VERSION=""

HOSTNAME="$(minikube ip)"
PROTOCOL="https"

LOGGING_LEVEL="INFO"
ENABLE_DEBUG="false"

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
    --hostname=*)
      HOSTNAME="${i#*=}"
      shift
      ;;
    --protocol=*)
      PROTOCOL="${i#*=}"
      shift
      ;;
    --debug)
      LOGGING_LEVEL="DEBUG"
      ENABLE_DEBUG="true"
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

if [[ -z $HOSTNAME ]]; then
  echo "Missing or invalid value for argument: --hostname"
  exit 1
fi

if [[ -z $PROTOCOL ]]; then
  echo "Missing or invalid value for argument: --protocol"
  exit 1
fi

COMMON_VALUES="image.tag=${VERSION},replicas=1,clientProtocol=https,clientDomain=${HOSTNAME},enableDebug=${ENABLE_DEBUG},loggingLevel=${LOGGING_LEVEL}"

helm upgrade --install service-authentication services/authentication/helm -n services --set image.repository=${REPOSITORY}/authentication,${COMMON_VALUES},clientWebUrl=${PROTOCOL}://${HOSTNAME},clientAuthUrl=${PROTOCOL}://${HOSTNAME}

helm upgrade --install service-accounts services/accounts/helm -n services --set image.repository=${REPOSITORY}/accounts,${COMMON_VALUES}

helm upgrade --install service-designs-query services/designs-query/helm -n services --set image.repository=${REPOSITORY}/designs-query,${COMMON_VALUES}
helm upgrade --install service-designs-command services/designs-command/helm -n services --set image.repository=${REPOSITORY}/designs-command,${COMMON_VALUES}
helm upgrade --install service-designs-aggregate services/designs-aggregate/helm -n services --set image.repository=${REPOSITORY}/designs-aggregate,${COMMON_VALUES}
helm upgrade --install service-designs-watch services/designs-watch/helm -n services --set image.repository=${REPOSITORY}/designs-watch,${COMMON_VALUES}
helm upgrade --install service-designs-render services/designs-render/helm -n services --set image.repository=${REPOSITORY}/designs-render,${COMMON_VALUES}

helm upgrade --install service-frontend services/frontend/helm -n services --set image.repository=${REPOSITORY}/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=${PROTOCOL}://${HOSTNAME},clientApiUrl=${PROTOCOL}://${HOSTNAME},enableDebug=false,loggingLevel=${LOGGING_LEVEL}
