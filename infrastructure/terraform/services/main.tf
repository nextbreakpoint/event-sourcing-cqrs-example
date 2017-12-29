##############################################################################
# Provider
##############################################################################

provider "aws" {
  region = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 0.1"
}

provider "template" {
  version = "~> 0.1"
}

provider "local" {
  version = "~> 0.1"
}

##############################################################################
# ECS Services and Tasks
##############################################################################

resource "aws_iam_role" "service_role" {
  name = "shop-service-role"
  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ecs.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy" "service_role_policy" {
  name = "shop-service-role-policy"
  role = "${aws_iam_role.service_role.id}"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:Describe*",
                "elasticloadbalancing:DeregisterInstancesFromLoadBalancer",
                "elasticloadbalancing:DeregisterTargets",
                "elasticloadbalancing:Describe*",
                "elasticloadbalancing:RegisterInstancesWithLoadBalancer",
                "elasticloadbalancing:RegisterTargets"
            ],
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_iam_role" "container_tasks_role" {
  name = "shop-container-role"
  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    },
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "s3.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy" "container_tasks_role_policy" {
  name = "shop-container-role-policy"
  role = "${aws_iam_role.container_tasks_role.id}"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage"
            ],
            "Resource": "*"
        },
        {
            "Action": [
                "s3:GetObject"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:s3:::${var.secrets_bucket_name}/*"
        }
    ]
}
EOF
}

data "template_file" "auth_template" {
  template = "${file("task-definitions/auth.json")}"

  vars {
    account_id         = "${var.account_id}"
    bucket_name        = "${var.secrets_bucket_name}"
  }
}

data "template_file" "designs_template" {
  template = "${file("task-definitions/designs.json")}"

  vars {
    account_id         = "${var.account_id}"
    bucket_name        = "${var.secrets_bucket_name}"
  }
}

data "template_file" "accounts_template" {
  template = "${file("task-definitions/accounts.json")}"

  vars {
    account_id         = "${var.account_id}"
    bucket_name        = "${var.secrets_bucket_name}"
  }
}

data "template_file" "web_template" {
  template = "${file("task-definitions/web.json")}"

  vars {
    account_id         = "${var.account_id}"
    bucket_name        = "${var.secrets_bucket_name}"
  }
}

resource "aws_ecs_service" "auth" {
  name            = "auth"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.auth.arn}"
  desired_count   = 1

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "auth" {
  family                = "auth"
  container_definitions = "${data.template_file.auth_template.rendered}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  depends_on      = ["aws_iam_role_policy.container_tasks_role_policy"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "designs" {
  name            = "designs"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.designs.arn}"
  desired_count   = 1

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "designs" {
  family                = "designs"
  container_definitions = "${data.template_file.designs_template.rendered}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  depends_on      = ["aws_iam_role_policy.container_tasks_role_policy"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "accounts" {
  name            = "accounts"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.accounts.arn}"
  desired_count   = 1

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "accounts" {
  family                = "accounts"
  container_definitions = "${data.template_file.accounts_template.rendered}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  depends_on      = ["aws_iam_role_policy.container_tasks_role_policy"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "web" {
  name            = "web"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.server.arn}"
  desired_count   = 1

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "server" {
  family                = "web"
  container_definitions = "${data.template_file.web_template.rendered}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  depends_on      = ["aws_iam_role_policy.container_tasks_role_policy"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

##############################################################################
# S3 Bucket objects
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
    filename = "environments/production/config/auth.json"
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
    filename = "environments/production/config/designs.json"
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
    filename = "environments/production/config/accounts.json"
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
    filename = "environments/production/config/web.json"
}

resource "aws_s3_bucket_object" "auth" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/auth.json"
  source = "environments/production/config/auth.json"
  etag   = "${md5(file("environments/production/config/auth.json"))}"
}

resource "aws_s3_bucket_object" "designs" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/designs.json"
  source = "environments/production/config/designs.json"
  etag   = "${md5(file("environments/production/config/designs.json"))}"
}

resource "aws_s3_bucket_object" "accounts" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/accounts.json"
  source = "environments/production/config/accounts.json"
  etag   = "${md5(file("environments/production/config/accounts.json"))}"
}

resource "aws_s3_bucket_object" "web" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/web.json"
  source = "environments/production/config/web.json"
  etag   = "${md5(file("environments/production/config/web.json"))}"
}
