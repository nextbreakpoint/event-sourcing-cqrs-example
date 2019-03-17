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

resource "local_file" "gateway_config" {
  content = <<EOF
{
  "host_port": 44000,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "client_keystore_path": "/keystores/keystore-client.jks",
  "client_keystore_secret": "${var.keystore_password}",

  "client_truststore_path": "/keystores/truststore-client.jks",
  "client_truststore_secret": "${var.truststore_password}",

  "client_verify_host": false,
  "client_keep_alive": true,

  "consul_host": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}",
  "consul_port": 8400,

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "server_auth_url": "https://shop-auth:43000",
  "server_accounts_url": "https://shop-accounts:43002",
  "server_designs_query_url": "https://shop-designs-query:43021",
  "server_designs_command_url": "https://shop-designs-command:43031",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/gateway.json"
}

resource "local_file" "auth_config" {
  content = <<EOF
{
  "host_port": 43000,

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

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "client_web_url": "https://${var.shop_hostname}:7443",
  "client_auth_url": "https://${var.shop_hostname}:7443",

  "server_auth_url": "https://shop-auth:43000",
  "server_accounts_url": "https://shop-accounts:43002",

  "github_url": "https://api.github.com",

  "oauth_login_url": "https://github.com/login",
  "oauth_token_path": "/oauth/access_token",
  "oauth_authorize_path": "/oauth/authorize",
  "oauth_authority": "user:email",

  "cookie_domain": "${var.hosted_zone_name}",

  "admin_users": ["${var.github_user_email}"],

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/auth.json"
}

resource "local_file" "designs_command_config" {
  content = <<EOF
{
  "host_port": 43031,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "kafka_keystore_location": "/keystores/kafka-keystore-client.jks",
  "kafka_truststore_location": "/keystores/kafka-truststore-client.jks",
  "kafka_keystore_password": "${data.terraform_remote_state.secrets.kafka-keystore-password}",
  "kafka_truststore_password": "${data.terraform_remote_state.secrets.kafka-truststore-password}",

  "events_topic": "designs-events",

  "message_source": "service-designs"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-command.json"
}

resource "local_file" "designs_processor_config" {
  content = <<EOF
{
  "host_port": 43011,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "cassandra_cluster": "${var.environment}-${var.colour}",
  "cassandra_keyspace": "designs",
  "cassandra_username": "${var.cassandra_username}",
  "cassandra_password": "${var.cassandra_password}",
  "cassandra_contactPoints": "cassandra1,cassandra2,cassandra3",
  "cassandra_port": 9042,

  "message_source": "service-designs",

  "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "kafka_keystore_location": "/keystores/kafka-keystore-client.jks",
  "kafka_truststore_location": "/keystores/kafka-truststore-client.jks",
  "kafka_keystore_password": "${data.terraform_remote_state.secrets.kafka-keystore-password}",
  "kafka_truststore_password": "${data.terraform_remote_state.secrets.kafka-truststore-password}",
  "kafka_group_id": "designs-processor",

  "events_topic": "designs-events",
  "view_topic": "designs-view",
  "sse_topic": "designs-sse"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-processor.json"
}

resource "local_file" "designs_query_config" {
  content = <<EOF
{
  "host_port": 43021,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "cassandra_cluster": "${var.environment}-${var.colour}",
  "cassandra_keyspace": "designs",
  "cassandra_username": "${var.cassandra_username}",
  "cassandra_password": "${var.cassandra_password}",
  "cassandra_contactPoints": "cassandra1,cassandra2,cassandra3",
  "cassandra_port": 9042,

  "max_execution_time_in_millis": 30000
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-query.json"
}

resource "local_file" "designs_sse_config_a" {
  content = <<EOF
{
  "host_port": 43041,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "kafka_keystore_location": "/keystores/kafka-keystore-client.jks",
  "kafka_truststore_location": "/keystores/kafka-truststore-client.jks",
  "kafka_keystore_password": "${data.terraform_remote_state.secrets.kafka-keystore-password}",
  "kafka_truststore_password": "${data.terraform_remote_state.secrets.kafka-truststore-password}",
  "kafka_group_id": "designs-sse-a",

  "sse_topic": "designs-sse"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-sse-a.json"
}

resource "local_file" "designs_sse_config_b" {
  content = <<EOF
{
  "host_port": 43041,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "kafka_keystore_location": "/keystores/kafka-keystore-client.jks",
  "kafka_truststore_location": "/keystores/kafka-truststore-client.jks",
  "kafka_keystore_password": "${data.terraform_remote_state.secrets.kafka-keystore-password}",
  "kafka_truststore_password": "${data.terraform_remote_state.secrets.kafka-truststore-password}",
  "kafka_group_id": "designs-sse-b",

  "sse_topic": "designs-sse"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-sse-b.json"
}

resource "local_file" "designs_sse_config_c" {
  content = <<EOF
{
  "host_port": 43041,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "kafka_keystore_location": "/keystores/kafka-keystore-client.jks",
  "kafka_truststore_location": "/keystores/kafka-truststore-client.jks",
  "kafka_keystore_password": "${data.terraform_remote_state.secrets.kafka-keystore-password}",
  "kafka_truststore_password": "${data.terraform_remote_state.secrets.kafka-truststore-password}",
  "kafka_group_id": "designs-sse-c",

  "sse_topic": "designs-sse"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-sse-c.json"
}

# resource "local_file" "designs_config" {
#   content = <<EOF
# {
#   "host_port": 43031,
#
#   "server_keystore_path": "/keystores/keystore-server.jks",
#   "server_keystore_secret": "${var.keystore_password}",
#
#   "jwt_keystore_path": "/keystores/keystore-auth.jceks",
#   "jwt_keystore_type": "jceks",
#   "jwt_keystore_secret": "${var.keystore_password}",
#
#   "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",
#
#   "graphite_reporter_enabled": true,
#   "graphite_host": "graphite",
#   "graphite_port": 2003,
#
#   "jdbc_url": "jdbc:mysql://shop-mysql:3306/designs?useSSL=false&allowPublicKeyRetrieval=true&nullNamePatternMatchesAll=true",
#   "jdbc_driver": "com.mysql.cj.jdbc.Driver",
#   "jdbc_username": "${var.mysql_username}",
#   "jdbc_password": "${var.mysql_password}",
#   "jdbc_max_pool_size": 200,
#   "jdbc_min_pool_size": 20,
#
#   "message_source": "service-designs",
#
#   "kafka_bootstrap_servers": "kafka1:9092,kafka2:9092,kafka3:9092",
#
#   "sse_topic": "designs-sse",
#
#   "max_execution_time_in_millis": 30000
# }
# EOF
#
#   filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs.json"
# }

resource "local_file" "accounts_config" {
  content = <<EOF
{
  "host_port": 43002,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://([a-z_-]+).${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": true,
  "graphite_host": "graphite",
  "graphite_port": 2003,

  "jdbc_url": "jdbc:mysql://shop-mysql:3306/shop?useSSL=false&allowPublicKeyRetrieval=true&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_username}",
  "jdbc_password": "${var.mysql_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/accounts.json"
}

resource "local_file" "web_config" {
  content = <<EOF
{
  "client_web_url": "https://${var.shop_hostname}:7443",
  "client_api_url": "https://${var.shop_hostname}:7443",
  "server_api_url": "https://shop-gateway:44000"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/web.json"
}

resource "local_file" "consul_config_a" {
  content = <<EOF
{
  "encrypt": "${data.terraform_remote_state.secrets.consul-secret}",
  "datacenter": "${var.consul_datacenter}",
  "cert_file": "/consul/config/server_cert.pem",
  "log_level": "info",
  "leave_on_terminate": true,
  "translate_wan_addrs": true,
  "disable_update_check": true,
  "enable_script_checks": true,
  "skip_leave_on_interrupt": true,
  "ports": { "https": -1, "http": 8400 },
  "dns_config": {
    "allow_stale": true,
    "max_stale": "1s",
    "service_ttl": {
      "*": "5s"
    }
  },
  "services": [{
    "name": "designs-sse",
    "tags": [
      "http-endpoint"
    ],
    "port": 443,
    "address": "${var.environment}-${var.colour}-swarm-worker-ext-pub-a.${var.hosted_zone_name}"
  }]
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/consul-a.json"
}
resource "local_file" "consul_config_b" {
  content = <<EOF
{
  "encrypt": "${data.terraform_remote_state.secrets.consul-secret}",
  "datacenter": "${var.consul_datacenter}",
  "cert_file": "/consul/config/server_cert.pem",
  "log_level": "info",
  "leave_on_terminate": true,
  "translate_wan_addrs": true,
  "disable_update_check": true,
  "enable_script_checks": true,
  "skip_leave_on_interrupt": true,
  "ports": { "https": -1, "http": 8400 },
  "dns_config": {
    "allow_stale": true,
    "max_stale": "1s",
    "service_ttl": {
      "*": "5s"
    }
  },
  "services": [{
    "name": "designs-sse",
    "tags": [
      "http-endpoint"
    ],
    "port": 443,
    "address": "${var.environment}-${var.colour}-swarm-worker-ext-pub-b.${var.hosted_zone_name}"
  }]
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/consul-b.json"
}
resource "local_file" "consul_config_c" {
  content = <<EOF
{
  "encrypt": "${data.terraform_remote_state.secrets.consul-secret}",
  "datacenter": "${var.consul_datacenter}",
  "cert_file": "/consul/config/server_cert.pem",
  "log_level": "info",
  "leave_on_terminate": true,
  "translate_wan_addrs": true,
  "disable_update_check": true,
  "enable_script_checks": true,
  "skip_leave_on_interrupt": true,
  "ports": { "https": -1, "http": 8400 },
  "dns_config": {
    "allow_stale": true,
    "max_stale": "1s",
    "service_ttl": {
      "*": "5s"
    }
  },
  "services": [{
    "name": "designs-sse",
    "tags": [
      "http-endpoint"
    ],
    "port": 443,
    "address": "${var.environment}-${var.colour}-swarm-worker-ext-pub-c.${var.hosted_zone_name}"
  }]
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/consul-c.json"
}

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
        set $upstream_api shop-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /auth {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /designs {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-gateway;
        proxy_pass https://$upstream_api:44000$request_uri;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /accounts {
        resolver 127.0.0.11 valid=30s;
        set $upstream_api shop-gateway;
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
