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

resource "local_file" "auth_config" {
  content = <<EOF
{
  "host_port": 3000,

  "github_client_id": "${var.github_client_id}",
  "github_client_secret": "${var.github_client_secret}",

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "client_keystore_path": "/keystores/keystore-client.jks",
  "client_keystore_secret": "${var.keystore_password}",

  "client_truststore_path": "/keystores/truststore-client.jks",
  "client_truststore_secret": "${var.truststore_password}",

  "client_verify_host": false,

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "client_web_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "client_auth_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "server_auth_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "server_accounts_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "github_url": "https://api.github.com",

  "oauth_login_url": "https://github.com/login",
  "oauth_token_path": "/oauth/access_token",
  "oauth_authorize_path": "/oauth/authorize",
  "oauth_authority": "user:email",

  "cookie_domain": "${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "admin_users": ["${var.github_user_email}"],

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/auth.json"
}

resource "local_file" "designs_config" {
  content = <<EOF
{
  "host_port": 3001,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "client_web_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "jdbc_url": "jdbc:mysql://${var.environment}-${var.colour}-shop-rds.${var.hosted_zone_name}:3306/designs?useSSL=false&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_verticle_username}",
  "jdbc_password": "${var.mysql_verticle_password}",
  "jdbc_liquibase_username": "${var.mysql_liquibase_username}",
  "jdbc_liquibase_password": "${var.mysql_liquibase_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "max_execution_time_in_millis": 30000
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs.json"
}

resource "local_file" "accounts_config" {
  content = <<EOF
{
  "host_port": 3002,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "client_web_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "jdbc_url": "jdbc:mysql://${var.environment}-${var.colour}-shop-rds.${var.hosted_zone_name}:3306/accounts?useSSL=false&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_verticle_username}",
  "jdbc_password": "${var.mysql_verticle_password}",
  "jdbc_liquibase_username": "${var.mysql_liquibase_username}",
  "jdbc_liquibase_password": "${var.mysql_liquibase_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/accounts.json"
}

resource "local_file" "web_config" {
  content = <<EOF
{
  "host_port": 8080,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "client_keystore_path": "/keystores/keystore-client.jks",
  "client_keystore_secret": "${var.keystore_password}",

  "client_truststore_path": "/keystores/truststore-client.jks",
  "client_truststore_secret": "${var.truststore_password}",

  "client_verify_host": false,

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "client_web_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "client_auth_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "client_designs_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "client_accounts_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "server_auth_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "server_designs_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",
  "server_accounts_url": "https://${var.environment}-${var.colour}-shop.${var.hosted_zone_name}",

  "csrf_secret": "changeme",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/web.json"
}

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
        resolver 127.0.0.1;
        set $upstream_auth shop-auth;
        proxy_pass https://$upstream_auth:3000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/designs {
        resolver 127.0.0.1;
        set $upstream_designs shop-designs;
        proxy_pass https://$upstream_designs:3001$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/accounts {
        resolver 127.0.0.1;
        set $upstream_accounts shop-accounts;
        proxy_pass https://$upstream_accounts:3002$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /watch {
        resolver 127.0.0.1;
        set $upstream_web shop-web;
        proxy_pass https://$upstream_web:8080$request_uri;
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
        resolver 127.0.0.1;
        set $upstream_web shop-web;
        proxy_pass https://$upstream_web:8080$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
  }
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/nginx/nginx.conf"
}
