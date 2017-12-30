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

##############################################################################
# Web servers
##############################################################################

resource "aws_security_group" "webserver" {
  name = "shop-nginx-security-group"
  description = "Shop NGINX security group"
  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress {
    from_port = 80
    to_port = 80
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 443
    to_port = 443
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["${var.aws_bastion_vpc_cidr}"]
  }

  ingress {
    from_port = 8301
    to_port = 8301
    protocol = "tcp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  ingress {
    from_port = 8301
    to_port = 8301
    protocol = "udp"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Stream = "${var.stream_tag}"
  }
}

data "template_file" "webserver_user_data" {
  template = "${file("provision/nginx.tpl")}"

  vars {
    aws_region              = "${var.aws_region}"
    environment             = "${var.environment}"
    bucket_name             = "${var.secrets_bucket_name}"
    consul_secret           = "${var.consul_secret}"
    consul_datacenter       = "${var.consul_datacenter}"
    consul_nodes            = "${replace(var.aws_network_private_subnet_cidr_a, "0/24", "90")},${replace(var.aws_network_private_subnet_cidr_b, "0/24", "90")},${replace(var.aws_network_private_subnet_cidr_c, "0/24", "90")}"
    consul_logfile          = "${var.consul_logfile}"
    security_groups         = "${aws_security_group.webserver.id}"
    hosted_zone_name        = "${var.hosted_zone_name}"
    filebeat_version        = "${var.filebeat_version}"
  }
}

resource "aws_iam_instance_profile" "webserver_profile" {
    name = "shop-nginx-profile"
    role = "${aws_iam_role.webserver_role.name}"
}

resource "aws_iam_role" "webserver_role" {
  name = "shop-nginx-role"

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

resource "aws_iam_role_policy" "webserver_role_policy" {
  name = "shop-nginx-role-policy"
  role = "${aws_iam_role.webserver_role.id}"

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

data "aws_ami" "webserver" {
  most_recent = true

  filter {
    name = "name"
    values = ["base-${var.base_version}-*"]
  }

  filter {
    name = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["${var.account_id}"]
}

resource "aws_launch_configuration" "webserver_launch_configuration" {
  name_prefix   = "shop-nginx-server-"
  instance_type = "${var.web_instance_type}"

  image_id = "${data.aws_ami.webserver.id}"

  associate_public_ip_address = "false"
  security_groups = ["${aws_security_group.webserver.id}"]
  key_name = "${var.key_name}"

  iam_instance_profile = "${aws_iam_instance_profile.webserver_profile.name}"

  user_data = "${data.template_file.webserver_user_data.rendered}"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "webserver_asg" {
  name                      = "shop-nginx-asg"
  max_size                  = 8
  min_size                  = 0
  health_check_grace_period = 300
  health_check_type         = "ELB"
  desired_capacity          = 2
  force_delete              = true
  launch_configuration      = "${aws_launch_configuration.webserver_launch_configuration.name}"

  vpc_zone_identifier = [
    "${data.terraform_remote_state.network.network-private-subnet-a-id}",
    "${data.terraform_remote_state.network.network-private-subnet-b-id}",
    "${data.terraform_remote_state.network.network-private-subnet-c-id}"
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
    value               = "shop-nginx-server"
    propagate_at_launch = true
  }

  timeouts {
    delete = "15m"
  }
}

resource "aws_security_group" "webserver_elb" {
  name = "shop-elb-security-group"
  description = "Shop ELB security group"
  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress {
    from_port = 80
    to_port = 80
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 443
    to_port = 443
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Stream = "${var.stream_tag}"
  }
}

data "aws_acm_certificate" "webserver_elb" {
  domain   = "*.${var.hosted_zone_name}"
  statuses = ["ISSUED"]
}

resource "aws_elb" "webserver_elb" {
  name = "shop-elb"
  security_groups = ["${aws_security_group.webserver_elb.id}"]
  subnets = [
    "${data.terraform_remote_state.network.network-public-subnet-a-id}",
    "${data.terraform_remote_state.network.network-public-subnet-b-id}",
    "${data.terraform_remote_state.network.network-public-subnet-c-id}"
  ]

  listener {
    instance_port = 80
    instance_protocol = "HTTP"
    lb_port = 80
    lb_protocol = "HTTP"
  }

  listener {
    instance_port       = 443
    instance_protocol   = "HTTPS"
    lb_port             = 443
    lb_protocol         = "HTTPS"
    ssl_certificate_id  = "${data.aws_acm_certificate.webserver_elb.arn}"
  }

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 3
    timeout = 10
    target = "TCP:80"
    interval = 30
  }

  cross_zone_load_balancing = true
  idle_timeout = 400
  connection_draining = true
  connection_draining_timeout = 400
  internal = false

  tags {
    Stream = "${var.stream_tag}"
  }
}

resource "aws_autoscaling_attachment" "webserver_asg" {
  autoscaling_group_name = "${aws_autoscaling_group.webserver_asg.id}"
  elb = "${aws_elb.webserver_elb.id}"
}

##############################################################################
# Route 53
##############################################################################

resource "aws_route53_record" "webserver_elb_public" {
  zone_id = "${var.hosted_zone_id}"
  name = "shop.${var.hosted_zone_name}"
  type = "A"

  alias {
    name = "${aws_elb.webserver_elb.dns_name}"
    zone_id = "${aws_elb.webserver_elb.zone_id}"
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "webserver_elb_network" {
  zone_id = "${data.terraform_remote_state.zones.network-hosted-zone-id}"
  name = "shop.${data.terraform_remote_state.zones.network-hosted-zone-name}"
  type = "A"

  alias {
    name = "${aws_elb.webserver_elb.dns_name}"
    zone_id = "${aws_elb.webserver_elb.zone_id}"
    evaluate_target_health = true
  }
}
