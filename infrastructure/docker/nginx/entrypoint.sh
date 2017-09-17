#!/bin/bash

env

if [ -n "$SECRETS_BUCKET_NAME" ]; then
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/nginx/nginx.crt /nginx/nginx.crt
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/nginx/nginx.key /nginx/nginx.key
  aws s3 cp s3://$SECRETS_BUCKET_NAME/environments/$ENVIRONMENT/config/nginx.conf /etc/nginx/nginx.conf
fi

cat <<EOF >/tmp/agent.json
{
  "datacenter": "terraform",
	"client_addr": "0.0.0.0",
	"data_dir": "/mnt/consul",
	"leave_on_terminate": true,
  "retry_join": ["consul.internal"],
	"dns_config": {
		"allow_stale": true,
		"max_stale": "1s"
	}
}
EOF
mv /tmp/agent.json /etc/service

/usr/local/bin/consul agent -config-dir=/etc/service &

nginx -g 'daemon off;'
