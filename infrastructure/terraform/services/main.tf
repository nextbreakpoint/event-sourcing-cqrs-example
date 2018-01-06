##############################################################################
# Providers
##############################################################################

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 0.1"
}

provider "template" {
  version = "~> 0.1"
}

##############################################################################
# Resources
##############################################################################

resource "aws_iam_role" "service" {
  name = "shop-service"

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

resource "aws_iam_role_policy" "service" {
  name = "shop-service"
  role = "${aws_iam_role.service.id}"

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

resource "aws_iam_role" "container" {
  name = "shop-container"

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

resource "aws_iam_role_policy" "container" {
  name = "shop-container"
  role = "${aws_iam_role.container.id}"

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
    account_id  = "${var.account_id}"
    bucket_name = "${var.secrets_bucket_name}"
  }
}

data "template_file" "designs_template" {
  template = "${file("task-definitions/designs.json")}"

  vars {
    account_id  = "${var.account_id}"
    bucket_name = "${var.secrets_bucket_name}"
  }
}

data "template_file" "accounts_template" {
  template = "${file("task-definitions/accounts.json")}"

  vars {
    account_id  = "${var.account_id}"
    bucket_name = "${var.secrets_bucket_name}"
  }
}

data "template_file" "web_template" {
  template = "${file("task-definitions/web.json")}"

  vars {
    account_id  = "${var.account_id}"
    bucket_name = "${var.secrets_bucket_name}"
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
  task_role_arn         = "${aws_iam_role.container.arn}"

  depends_on = ["aws_iam_role_policy.container"]

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
  task_role_arn         = "${aws_iam_role.container.arn}"

  depends_on = ["aws_iam_role_policy.container"]

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
  task_role_arn         = "${aws_iam_role.container.arn}"

  depends_on = ["aws_iam_role_policy.container"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "web" {
  name            = "web"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.web.arn}"
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

resource "aws_ecs_task_definition" "web" {
  family                = "web"
  container_definitions = "${data.template_file.web_template.rendered}"
  task_role_arn         = "${aws_iam_role.container.arn}"

  depends_on = ["aws_iam_role_policy.container"]

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb, %sc]", var.aws_region, var.aws_region, var.aws_region)}"
  }
}

resource "aws_s3_bucket_object" "auth" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/auth.json"
  source = "../../secrets/environments/production/config/auth.json"
  etag   = "${md5(file("../../secrets/environments/production/config/auth.json"))}"
}

resource "aws_s3_bucket_object" "designs" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/designs.json"
  source = "../../secrets/environments/production/config/designs.json"
  etag   = "${md5(file("../../secrets/environments/production/config/designs.json"))}"
}

resource "aws_s3_bucket_object" "accounts" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/accounts.json"
  source = "../../secrets/environments/production/config/accounts.json"
  etag   = "${md5(file("../../secrets/environments/production/config/accounts.json"))}"
}

resource "aws_s3_bucket_object" "web" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/production/config/web.json"
  source = "../../secrets/environments/production/config/web.json"
  etag   = "${md5(file("../../secrets/environments/production/config/web.json"))}"
}
