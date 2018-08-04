##############################################################################
# Providers
##############################################################################

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 0.1"
}

##############################################################################
# Resources
##############################################################################

resource "aws_security_group" "shop" {
  name   = "${var.environment}-${var.colour}-shop-rds"
  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress = {
    from_port = 3306
    to_port   = 3306

    protocol = "tcp"

    cidr_blocks = [
      "${var.aws_network_vpc_cidr}",
      "${var.aws_bastion_vpc_cidr}",
      "${var.aws_openvpn_vpc_cidr}"
    ]
  }

  tags = {
    Environment = "${var.environment}"
    Colour      = "${var.colour}"
  }
}

resource "aws_db_instance" "shop" {
  availability_zone      = "${format("%sa", var.aws_region)}"
  allocated_storage      = 5
  storage_type           = "gp2"
  engine                 = "mysql"
  engine_version         = "5.7.17"
  instance_class         = "db.t2.small"
  name                   = "${var.environment}_${var.colour}_shop"
  username               = "${var.rds_mysql_username}"
  password               = "${var.rds_mysql_password}"
  db_subnet_group_name   = "${var.environment}-${var.colour}-shop-rds"
  parameter_group_name   = "default.mysql5.7"
  publicly_accessible    = false
  apply_immediately      = true
  skip_final_snapshot    = true
  vpc_security_group_ids = ["${aws_security_group.shop.id}"]

  depends_on = ["aws_db_subnet_group.shop"]

  tags {
    Environment = "${var.environment}"
    Colour      = "${var.colour}"
  }
}

resource "aws_db_subnet_group" "shop" {
  name = "${var.environment}-${var.colour}-shop-rds"

  subnet_ids = [
    "${data.terraform_remote_state.network.network-private-subnet-a-id}",
    "${data.terraform_remote_state.network.network-private-subnet-b-id}",
    "${data.terraform_remote_state.network.network-private-subnet-c-id}"
  ]

  tags {
    Environment = "${var.environment}"
    Colour      = "${var.colour}"
  }
}

resource "aws_route53_record" "shop" {
  zone_id = "${var.hosted_zone_id}"
  name    = "${var.environment}-${var.colour}-shop-rds.${var.hosted_zone_name}"
  type    = "CNAME"
  ttl     = "60"

  records = [
    "${aws_db_instance.shop.address}"
  ]
}
