#!/bin/bash

set +e

kubectl -n platform create secret generic nginx --from-file server_cert.pem=secrets/nginx_server_cert.pem --from-file server_key.pem=secrets/nginx_server_key.pem
