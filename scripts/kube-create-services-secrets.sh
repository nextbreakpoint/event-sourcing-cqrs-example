#!/bin/bash

set +e

if [[ -z $GITHUB_ACCOUNT_ID ]]; then
  echo "Missing variable: GITHUB_ACCOUNT_ID"
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

kubectl -n services create secret generic keystore-auth.jceks --from-file=secrets/keystore_auth.jceks

kubectl -n services create secret generic authentication --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal GITHUB_ACCOUNT_ID=$GITHUB_ACCOUNT_ID --from-literal GITHUB_CLIENT_ID=$GITHUB_CLIENT_ID --from-literal GITHUB_CLIENT_SECRET=$GITHUB_CLIENT_SECRET
kubectl -n services create secret generic accounts --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
kubectl -n services create secret generic designs-query --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password
kubectl -n services create secret generic designs-command --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
kubectl -n services create secret generic designs-aggregate --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal DATABASE_USERNAME=verticle --from-literal DATABASE_PASSWORD=password
kubectl -n services create secret generic designs-watch --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret
kubectl -n services create secret generic designs-render --from-file keystore_auth.jceks=secrets/keystore_auth.jceks --from-literal KEYSTORE_SECRET=secret --from-literal AWS_ACCESS_KEY_ID=admin --from-literal AWS_SECRET_ACCESS_KEY=password

