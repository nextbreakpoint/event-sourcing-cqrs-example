#!/bin/bash

set -e

helm upgrade --install service-authentication services/authentication/helm -n services --set image.repository=integration/authentication,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),clientWebUrl=https://$(minikube ip):443,clientAuthUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}

helm upgrade --install service-accounts services/accounts/helm -n services --set image.repository=integration/accounts,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

helm upgrade --install service-designs-query services/designs-query/helm -n services --set image.repository=integration/designs-query,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
helm upgrade --install service-designs-command services/designs-command/helm -n services --set image.repository=integration/designs-command,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
helm upgrade --install service-designs-aggregate services/designs-aggregate/helm -n services --set image.repository=integration/designs-aggregate,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
helm upgrade --install service-designs-notify services/designs-notify/helm -n services --set image.repository=integration/designs-notify,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}
helm upgrade --install service-designs-render services/designs-render/helm -n services --set image.repository=integration/designs-render,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

helm upgrade --install service-gateway services/gateway/helm -n services --set image.repository=integration/gateway,image.tag=${VERSION},replicas=1,clientDomain=$(minikube ip),enableDebug=false,loggingLevel=${LOGGING_LEVEL}

helm upgrade --install service-frontend services/frontend/helm -n services --set image.repository=integration/frontend,image.tag=${VERSION},replicas=1,clientWebUrl=https://$(minikube ip):443,clientApiUrl=https://$(minikube ip):443,enableDebug=false,loggingLevel=${LOGGING_LEVEL}