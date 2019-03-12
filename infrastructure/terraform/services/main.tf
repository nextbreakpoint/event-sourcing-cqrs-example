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

  "client_web_url": "https://${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:7443",
  "client_auth_url": "https://${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:7443",

  "server_auth_url": "https://shop-auth:43000",
  "server_accounts_url": "https://shop-accounts:43002",

  "github_url": "https://api.github.com",

  "oauth_login_url": "https://github.com/login",
  "oauth_token_path": "/oauth/access_token",
  "oauth_authorize_path": "/oauth/authorize",
  "oauth_authority": "user:email",

  "cookie_domain": "${var.environment}-${var.colour}-swarm-worker.${var.hosted_zone_name}",

  "admin_users": ["${var.github_user_email}"],

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}",
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

  "origin_pattern": "https://${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:9092",

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

  "origin_pattern": "https://${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "cassandra_cluster": "${var.environment}-${var.colour}",
  "cassandra_keyspace": "designs",
  "cassandra_username": "${var.cassandra_username}",
  "cassandra_password": "${var.cassandra_password}",
  "cassandra_contactPoint": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}",
  "cassandra_port": 9042,

  "message_source": "service-designs",

  "kafka_bootstrap_servers": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:9092",
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

  "origin_pattern": "https://${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "cassandra_cluster": "${var.environment}-${var.colour}",
  "cassandra_keyspace": "designs",
  "cassandra_username": "${var.cassandra_username}",
  "cassandra_password": "${var.cassandra_password}",
  "cassandra_contactPoint": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}",
  "cassandra_port": 9042,

  "max_execution_time_in_millis": 30000
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-query.json"
}

resource "local_file" "designs_sse_config" {
  content = <<EOF
{
  "host_port": 43041,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "kafka_bootstrap_servers": "${var.environment}-${var.colour}-swarm-worker-int:9092",
  "kafka_group_id": "designs-sse",

  "sse_topic": "designs-sse"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs-sse.json"
}

resource "local_file" "designs_config" {
  content = <<EOF
{
  "host_port": 43031,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "origin_pattern": "https://${var.hosted_zone_name}(:[0-9]+)?",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "jdbc_url": "jdbc:mysql://shop-mysql:43306/designs?useSSL=false&allowPublicKeyRetrieval=true&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_username}",
  "jdbc_password": "${var.mysql_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,

  "message_source": "service-designs",

  "kafka_bootstrap_servers": "${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:9092",

  "sse_topic": "designs-sse",

  "max_execution_time_in_millis": 30000
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/designs.json"
}

resource "local_file" "accounts_config" {
  content = <<EOF
{
  "host_port": 43002,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "${var.keystore_password}",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "${var.keystore_password}",

  "client_web_url": "https://${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:7443",

  "graphite_reporter_enabled": false,
  "graphite_host": "http://${var.environment}-${var.colour}-swarm-manager.${var.hosted_zone_name}",
  "graphite_port": 2003,

  "jdbc_url": "jdbc:mysql://shop-mysql:43306/designs?useSSL=false&allowPublicKeyRetrieval=true&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_username}",
  "jdbc_password": "${var.mysql_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/accounts.json"
}

resource "local_file" "web_config" {
  content = <<EOF
{
  "client_web_url": "https://${var.hosted_zone_name}",
  "client_api_url": "https://${var.hosted_zone_name}",
  "server_api_url": "https://${var.environment}-${var.colour}-swarm-worker-int.${var.hosted_zone_name}:4400"
}
EOF

  filename = "../../secrets/environments/${var.environment}/${var.colour}/config/web.json"
}
