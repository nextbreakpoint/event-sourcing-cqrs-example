##############################################################################
# Providers
##############################################################################

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 0.1"
}

provider "local" {
  version = "~> 0.1"
}

##############################################################################
# Resources
##############################################################################

resource "local_file" "nginx_config" {
  content = <<EOF
worker_processes 4;
worker_rlimit_nofile 8192;

events {
  worker_connections 4096;
}

user www-data www-data;

http {
  ssl_session_cache     shared:SSL:10m;
  ssl_session_timeout   10m;

  server {
    listen 80;
    server_name ${var.environment}-${var.colour}-shop.${var.hosted_zone_name};
    return 301 https://$$server_name$$request_uri;
  }

  server {
    listen 443 ssl;
    server_name ${var.environment}-${var.colour}-shop.${var.hosted_zone_name};

    ssl_certificate     /etc/nginx/ca_and_server_cert.pem;
    ssl_certificate_key /etc/nginx/server_key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location /auth {
        resolver 127.0.0.11;
        set $upstream_auth shop-auth;
        proxy_pass https://$upstream_auth:43000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/designs {
        resolver 127.0.0.11;
        set $upstream_designs shop-designs;
        proxy_pass https://$upstream_designs:43001$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/accounts {
        resolver 127.0.0.11;
        set $upstream_accounts shop-accounts;
        proxy_pass https://$upstream_accounts:43002$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /watch {
        resolver 127.0.0.11;
        set $upstream_web shop-web;
        proxy_pass https://$upstream_web:48080$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
        proxy_buffering off;
        proxy_cache off;
    }

    location / {
        resolver 127.0.0.11;
        set $upstream_web shop-web;
        proxy_pass https://$upstream_web:48080$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx.conf"
}
