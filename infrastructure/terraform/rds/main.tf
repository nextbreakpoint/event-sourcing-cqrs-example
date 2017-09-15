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
# RDS configuration
##############################################################################

resource "aws_security_group" "services" {
  name = "rds-services-security-group"
  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress = {
    from_port = 3306
    to_port = 3306

    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    stream = "${var.stream_tag}"
  }
}

resource "aws_db_instance" "services" {
  availability_zone    = "${format("%sa", var.aws_region)}"
  allocated_storage    = 5
  storage_type         = "gp2"
  engine               = "mysql"
  engine_version       = "5.7.17"
  instance_class       = "db.t2.medium"
  name                 = "events"
  username             = "test"
  password             = "password"
  db_subnet_group_name = "rds-services-subnet-group"
  parameter_group_name = "default.mysql5.7"
  publicly_accessible  = true
  apply_immediately    = true
  skip_final_snapshot  = true
  vpc_security_group_ids = ["${aws_security_group.services.id}"]

  depends_on = ["aws_db_subnet_group.services"]

  tags {
    stream = "${var.stream_tag}"
  }
}

resource "aws_db_subnet_group" "services" {
  name       = "rds-services-subnet-group"

  subnet_ids = [
    "${data.terraform_remote_state.vpc.network-private-subnet-a-id}",
    "${data.terraform_remote_state.vpc.network-private-subnet-b-id}"
  ]

  tags {
    stream = "${var.stream_tag}"
  }
}

##############################################################################
# Route 53
##############################################################################

resource "aws_route53_record" "services" {
  zone_id = "${data.terraform_remote_state.vpc.hosted-zone-id}"
  name = "rds-services.${var.hosted_zone_name}"
  type = "CNAME"
  ttl = "300"

  records = [
    "${aws_db_instance.services.address}"
  ]
}
