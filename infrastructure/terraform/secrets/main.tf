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
  "server_keystore_secret": "secret",

  "client_keystore_path": "/keystores/keystore-client.jks",
  "client_keystore_secret": "secret",

  "client_truststore_path": "/keystores/truststore-client.jks",
  "client_truststore_secret": "secret",

  "client_verify_host": false,

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "secret",

  "client_web_url": "https://shop.${var.hosted_zone_name}",
  "client_auth_url": "https://shop.${var.hosted_zone_name}",

  "server_auth_url": "https://shop.${var.hosted_zone_name}",
  "server_accounts_url": "https://shop.${var.hosted_zone_name}",

  "github_url": "https://api.github.com",

  "oauth_login_url": "https://github.com/login",
  "oauth_token_path": "/oauth/access_token",
  "oauth_authorize_path": "/oauth/authorize",
  "oauth_authority": "user:email",

  "cookie_domain": "shop.${var.hosted_zone_name}",

  "admin_users": ["${var.github_user_email}"],

  "graphite_reporter_enabled": false,
  "graphite_host": "graphite.service.terraform.consul",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/production/config/auth.json"
}

resource "local_file" "designs_config" {
  content = <<EOF
{
  "host_port": 3001,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "secret",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "secret",

  "client_web_url": "https://shop.${var.hosted_zone_name}",

  "jdbc_url": "jdbc:mysql://shop-rds.${var.hosted_zone_name}:3306/designs?useSSL=false&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_verticle_username}",
  "jdbc_password": "${var.mysql_verticle_password}",
  "jdbc_liquibase_username": "${var.mysql_liquibase_username}",
  "jdbc_liquibase_password": "${var.mysql_liquibase_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,

  "graphite_reporter_enabled": false,
  "graphite_host": "graphite.service.terraform.consul",
  "graphite_port": 2003,

  "max_execution_time_in_millis": 30000
}
EOF

  filename = "../../secrets/environments/production/config/designs.json"
}

resource "local_file" "accounts_config" {
  content = <<EOF
{
  "host_port": 3002,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "secret",

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "secret",

  "client_web_url": "https://shop.${var.hosted_zone_name}",

  "jdbc_url": "jdbc:mysql://shop-rds.${var.hosted_zone_name}:3306/accounts?useSSL=false&nullNamePatternMatchesAll=true",
  "jdbc_driver": "com.mysql.cj.jdbc.Driver",
  "jdbc_username": "${var.mysql_verticle_username}",
  "jdbc_password": "${var.mysql_verticle_password}",
  "jdbc_liquibase_username": "${var.mysql_liquibase_username}",
  "jdbc_liquibase_password": "${var.mysql_liquibase_password}",
  "jdbc_max_pool_size": 200,
  "jdbc_min_pool_size": 20,

  "graphite_reporter_enabled": false,
  "graphite_host": "graphite.service.terraform.consul",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/production/config/accounts.json"
}

resource "local_file" "web_config" {
  content = <<EOF
{
  "host_port": 8080,

  "server_keystore_path": "/keystores/keystore-server.jks",
  "server_keystore_secret": "secret",

  "client_keystore_path": "/keystores/keystore-client.jks",
  "client_keystore_secret": "secret",

  "client_truststore_path": "/keystores/truststore-client.jks",
  "client_truststore_secret": "secret",

  "client_verify_host": false,

  "jwt_keystore_path": "/keystores/keystore-auth.jceks",
  "jwt_keystore_type": "jceks",
  "jwt_keystore_secret": "secret",

  "client_web_url": "https://shop.${var.hosted_zone_name}",
  "client_auth_url": "https://shop.${var.hosted_zone_name}",
  "client_designs_url": "https://shop.${var.hosted_zone_name}",
  "client_accounts_url": "https://shop.${var.hosted_zone_name}",

  "server_auth_url": "https://shop.${var.hosted_zone_name}",
  "server_designs_url": "https://shop.${var.hosted_zone_name}",
  "server_accounts_url": "https://shop.${var.hosted_zone_name}",

  "csrf_secret": "changeme",

  "graphite_reporter_enabled": false,
  "graphite_host": "graphite.service.terraform.consul",
  "graphite_port": 2003
}
EOF

  filename = "../../secrets/environments/production/config/web.json"
}
