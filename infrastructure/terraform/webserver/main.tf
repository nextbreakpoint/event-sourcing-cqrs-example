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

resource "aws_security_group" "webserver" {
  name        = "shop-webserver"
  description = "Shop NGINX security group"
  vpc_id      = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["${var.aws_bastion_vpc_cidr}"]
  }

  ingress {
    from_port   = 8301
    to_port     = 8301
    protocol    = "tcp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  ingress {
    from_port   = 8301
    to_port     = 8301
    protocol    = "udp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  egress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Stream = "${var.stream_tag}"
  }
}

resource "aws_iam_role" "webserver" {
  name = "shop-webserver"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
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
EOF
}

resource "aws_iam_role_policy" "webserver" {
  name = "shop-webserver"
  role = "${aws_iam_role.webserver.id}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "ec2:DescribeInstances"
      ],
      "Effect": "Allow",
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

resource "aws_iam_instance_profile" "webserver" {
  name = "shop-webserver"
  role = "${aws_iam_role.webserver.name}"
}

data "template_file" "webserver" {
  template = "${file("provision/nginx.tpl")}"

  vars {
    aws_region        = "${var.aws_region}"
    environment       = "${var.environment}"
    bucket_name       = "${var.secrets_bucket_name}"
    consul_secret     = "${var.consul_secret}"
    consul_datacenter = "${var.consul_datacenter}"
    consul_nodes      = "${replace(var.aws_network_private_subnet_cidr_a, "0/24", "90")},${replace(var.aws_network_private_subnet_cidr_b, "0/24", "90")},${replace(var.aws_network_private_subnet_cidr_c, "0/24", "90")}"
    security_groups   = "${aws_security_group.webserver.id}"
    hosted_zone_name  = "${var.hosted_zone_name}"
    filebeat_version  = "${var.filebeat_version}"
  }
}

data "aws_ami" "webserver" {
  most_recent = true

  filter {
    name   = "name"
    values = ["base-${var.base_version}-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["${var.account_id}"]
}

resource "aws_launch_configuration" "webserver" {
  name_prefix                 = "shop-webserver-"
  image_id                    = "${data.aws_ami.webserver.id}"
  instance_type               = "${var.web_instance_type}"
  security_groups             = ["${aws_security_group.webserver.id}"]
  iam_instance_profile        = "${aws_iam_instance_profile.webserver.name}"
  user_data                   = "${data.template_file.webserver.rendered}"
  key_name                    = "${var.key_name}"
  associate_public_ip_address = "false"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "webserver" {
  name                      = "shop-webserver"
  max_size                  = 8
  min_size                  = 0
  health_check_grace_period = 300
  health_check_type         = "ELB"
  desired_capacity          = 2
  force_delete              = true
  launch_configuration      = "${aws_launch_configuration.webserver.name}"

  vpc_zone_identifier = [
    "${data.terraform_remote_state.network.network-private-subnet-a-id}",
    "${data.terraform_remote_state.network.network-private-subnet-b-id}",
    "${data.terraform_remote_state.network.network-private-subnet-c-id}",
  ]

  lifecycle {
    create_before_destroy = true
  }

  tag {
    key                 = "Stream"
    value               = "${var.stream_tag}"
    propagate_at_launch = true
  }

  tag {
    key                 = "Name"
    value               = "shop-webserver"
    propagate_at_launch = true
  }

  timeouts {
    delete = "15m"
  }
}

resource "aws_alb_target_group" "webserver_http" {
  name     = "shop-webserver-http"
  port     = 80
  protocol = "HTTP"
  vpc_id   = "${data.terraform_remote_state.vpc.network-vpc-id}"

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200,301,400,404"
  }
}

resource "aws_alb_target_group" "webserver_https" {
  name     = "shop-webserver-https"
  port     = 443
  protocol = "HTTPS"
  vpc_id   = "${data.terraform_remote_state.vpc.network-vpc-id}"

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200,301,400,404"
  }
}

resource "aws_alb_listener_rule" "webserver_http" {
  listener_arn = "${data.terraform_remote_state.lb.lb-public-listener-http-arn}"
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.webserver_http.arn}"
  }

  condition {
    field  = "host-header"
    values = ["shop.${var.hosted_zone_name}"]
  }
}

resource "aws_alb_listener_rule" "webserver_https" {
  listener_arn = "${data.terraform_remote_state.lb.lb-public-listener-https-arn}"
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.webserver_https.arn}"
  }

  condition {
    field  = "host-header"
    values = ["shop.${var.hosted_zone_name}"]
  }
}

resource "aws_autoscaling_attachment" "webserver_http" {
  autoscaling_group_name = "${aws_autoscaling_group.webserver.id}"
  alb_target_group_arn   = "${aws_alb_target_group.webserver_http.arn}"
}

resource "aws_autoscaling_attachment" "webserver_https" {
  autoscaling_group_name = "${aws_autoscaling_group.webserver.id}"
  alb_target_group_arn   = "${aws_alb_target_group.webserver_https.arn}"
}

resource "aws_route53_record" "webserver" {
  zone_id = "${var.hosted_zone_id}"
  name    = "shop.${var.hosted_zone_name}"
  type    = "A"

  alias {
    name                   = "${data.terraform_remote_state.lb.lb-public-alb-dns-name}"
    zone_id                = "${data.terraform_remote_state.lb.lb-public-alb-zone-id}"
    evaluate_target_health = true
  }
}
