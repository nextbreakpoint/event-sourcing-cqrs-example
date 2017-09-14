##############################################################################
# Provider
##############################################################################

provider "aws" {
  region = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 0.1"
}

provider "terraform" {
  version = "~> 0.1"
}

##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "nextbreakpoint-terraform-state"
    region = "eu-west-1"
    key = "services-ecs.tfstate"
  }
}

data "terraform_remote_state" "vpc" {
    backend = "s3"
    config {
        bucket = "nextbreakpoint-terraform-state"
        region = "${var.aws_region}"
        key = "vpc.tfstate"
    }
}

data "terraform_remote_state" "network" {
    backend = "s3"
    config {
        bucket = "nextbreakpoint-terraform-state"
        region = "${var.aws_region}"
        key = "network.tfstate"
    }
}

data "terraform_remote_state" "ecs" {
    backend = "s3"
    config {
        bucket = "nextbreakpoint-terraform-state"
        region = "${var.aws_region}"
        key = "ecs.tfstate"
    }
}

##############################################################################
# ECS
##############################################################################

resource "aws_iam_role" "server_role" {
  name               = "Service"
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

resource "aws_iam_role_policy" "server_role_policy" {
  name = "ServicePolicy"
  role = "${aws_iam_role.server_role.id}"

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
                "elasticloadbalancing:RegisterTargets",
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage"
            ],
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_iam_role" "container_role" {
  name               = "Container"
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

resource "aws_iam_role_policy" "container_role_policy" {
  name = "ContainerPolicy"
  role = "${aws_iam_role.container_role.id}"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:Describe*",
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage"
            ],
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_iam_role" "container_tasks_role" {
  name               = "ContainerTasks"
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
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy" "container_tasks_role_policy" {
  name = "ContainerTasksPolicy"
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
        }
    ]
}
EOF
}

resource "aws_ecs_service" "accounts" {
  name            = "accounts"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.accounts.arn}"
  desired_count   = 2
  depends_on      = ["aws_iam_role_policy.container_role_policy"]

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "accounts" {
  family                = "accounts"
  container_definitions = "${file("task-definitions/accounts.json")}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  volume {
    name = "config"
    host_path = "/config/accounts"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "designs" {
  name            = "designs"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.designs.arn}"
  desired_count   = 2
  depends_on      = ["aws_iam_role_policy.container_role_policy"]

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "designs" {
  family                = "designs"
  container_definitions = "${file("task-definitions/designs.json")}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  volume {
    name = "config"
    host_path = "/config/designs"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "auth" {
  name            = "auth"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.auth.arn}"
  desired_count   = 2
  depends_on      = ["aws_iam_role_policy.container_role_policy"]

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "auth" {
  family                = "auth"
  container_definitions = "${file("task-definitions/auth.json")}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  volume {
    name = "config"
    host_path = "/config/auth"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_service" "web" {
  name            = "web"
  cluster         = "${data.terraform_remote_state.ecs.ecs-cluster-id}"
  task_definition = "${aws_ecs_task_definition.server.arn}"
  desired_count   = 2
  depends_on      = ["aws_iam_role_policy.server_role_policy"]

  placement_strategy {
    type  = "spread"
    field = "instanceId"
  }

  iam_role        = "${aws_iam_role.server_role.arn}"

  load_balancer {
    elb_name       = "${data.terraform_remote_state.ecs.ecs-cluster-elb-name}"
    container_name = "web"
    container_port = 8080
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}

resource "aws_ecs_task_definition" "server" {
  family                = "web"
  container_definitions = "${file("task-definitions/web.json")}"
  task_role_arn         = "${aws_iam_role.container_tasks_role.arn}"

  volume {
    name = "config"
    host_path = "/config/web"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "${format("attribute:ecs.availability-zone in [%sa, %sb]", var.aws_region, var.aws_region)}"
  }
}
