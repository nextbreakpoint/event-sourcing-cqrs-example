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

provider "template" {
  version = "~> 0.1"
}

provider "null" {
  version = "~> 0.1"
}

##############################################################################
# Databases maintenance servers
##############################################################################

resource "aws_security_group" "databases_maintenance_server" {
  name = "databases-maintenance-security-group"
  description = "databases maintenance security group"
  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 80
    to_port = 80
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 443
    to_port = 443
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["${var.aws_network_vpc_cidr}"]
  }

  tags {
    Stream = "${var.stream_tag}"
  }
}

resource "aws_iam_instance_profile" "databases_maintenance_server_profile" {
    name = "databases-maintenance-server-profile"
    role = "${aws_iam_role.databases_maintenance_server_role.name}"
}

resource "aws_iam_role" "databases_maintenance_server_role" {
  name = "databases-maintenance-server-role"

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
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "databases_maintenance_server_role_policy" {
  name = "databases-maintenance-server-role-policy"
  role = "${aws_iam_role.databases_maintenance_server_role.id}"

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
    }
  ]
}
EOF
}

data "template_file" "configure_databases" {
  template = "${file("provision/configure_databases.tpl")}"
}

data "template_file" "terraform_provider" {
  template = "${file("provision/provider.tf")}"

  vars = {
    endpoint = "rds-services.${var.hosted_zone_name}:3306"
    username = "${data.terraform_remote_state.rds.rds-services-username}"
    password = "${data.terraform_remote_state.rds.rds-services-password}"
  }
}

resource "aws_instance" "databases_maintenance_server_a" {
  instance_type = "t2.micro"

  # Lookup the correct AMI based on the region we specified
  ami = "${lookup(var.amazon_ubuntu_amis, var.aws_region)}"

  subnet_id = "${data.terraform_remote_state.vpc.network-private-subnet-a-id}"
  associate_public_ip_address = "false"
  security_groups = ["${aws_security_group.databases_maintenance_server.id}"]
  key_name = "${var.key_name}"

  iam_instance_profile = "${aws_iam_instance_profile.databases_maintenance_server_profile.id}"

  connection {
    # The default username for our AMI
    user = "ubuntu"
    type = "ssh"
    # The path to your keyfile
    private_key = "${file(var.key_path)}"
    bastion_user = "ec2-user"
    bastion_host = "bastion.${var.public_hosted_zone_name}"
  }

  tags {
    Name = "databases-maintenance-server-a"
    Stream = "${var.stream_tag}"
  }

  provisioner "file" {
      content = "${data.template_file.terraform_provider.rendered}"
      destination = "/tmp/provider.tf"
  }

  provisioner "file" {
      source = "provision/main.tf"
      destination = "/tmp/main.tf"
  }

  provisioner "file" {
      content = "${data.template_file.configure_databases.rendered}"
      destination = "/tmp/configure_databases.sh"
  }

  provisioner "remote-exec" {
      inline = [
        "sudo sh /tmp/configure_databases.sh"
      ]
  }
}
