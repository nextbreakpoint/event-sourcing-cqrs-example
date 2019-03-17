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

resource "local_file" "nginx_config_int" {
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
    server_name ${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name} ${var.environment}-${var.colour}-shop.${var.hosted_zone_name};
    return 301 https://$$server_name$$request_uri;
  }

  server {
    listen 443 ssl;
    server_name ${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name} ${var.environment}-${var.colour}-shop.${var.hosted_zone_name};

    ssl_certificate     /etc/nginx/ca_and_server_cert.pem;
    ssl_certificate_key /etc/nginx/server_key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location /watch {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-api-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /auth {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-api-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /designs {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-api-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /accounts {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-api-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        resolver 127.0.0.11 valid=30s;
        set $upstream_web shop-web;
        proxy_pass https://$upstream_web:8080$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx_int.conf"
}

resource "local_file" "nginx_config_ext_a" {
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
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-a.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-a.${var.hosted_zone_name};
    return 301 https://$$server_name$$request_uri;
  }

  server {
    listen 443 ssl;
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-a.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-a.${var.hosted_zone_name};

    ssl_certificate     /etc/nginx/ca_and_server_cert.pem;
    ssl_certificate_key /etc/nginx/server_key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location /watch/designs {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-designs-sse-a;
        proxy_pass https://$upstream_api:43041$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        keepalive_timeout 300s;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx_ext_a.conf"
}

resource "local_file" "nginx_config_ext_b" {
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
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-b.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-b.${var.hosted_zone_name};
    return 301 https://$$server_name$$request_uri;
  }

  server {
    listen 443 ssl;
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-b.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-b.${var.hosted_zone_name};

    ssl_certificate     /etc/nginx/ca_and_server_cert.pem;
    ssl_certificate_key /etc/nginx/server_key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location /watch/designs {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-designs-sse-b;
        proxy_pass https://$upstream_api:43041$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        keepalive_timeout 300s;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx_ext_b.conf"
}

resource "local_file" "nginx_config_ext_c" {
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
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-c.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-c.${var.hosted_zone_name};
    return 301 https://$$server_name$$request_uri;
  }

  server {
    listen 443 ssl;
    server_name ${var.environment}-${var.colour}-swarm-worker-ext-pub-c.${var.hosted_zone_name} ${var.environment}-${var.colour}-swarm-worker-ext-c.${var.hosted_zone_name};

    ssl_certificate     /etc/nginx/ca_and_server_cert.pem;
    ssl_certificate_key /etc/nginx/server_key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location /watch/designs {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-designs-sse-c;
        proxy_pass https://$upstream_api:43041$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        keepalive_timeout 300s;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx_ext_c.conf"
}
