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

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${var.environment}-${var.colour}-shop"
  engine               = "redis"
  engine_version       = "3.2.4"
  node_type            = "cache.t2.medium"
  port                 = 6379
  num_cache_nodes      = 1
  parameter_group_name = "default.redis3.2"
  subnet_group_name    = "${aws_elasticache_subnet_group.redis.name}"
  security_group_ids   = ["${aws_security_group.redis.id}"]

  tags {
    Environment = "${var.environment}"
    Colour      = "${var.colour}"
  }
}

resource "aws_elasticache_subnet_group" "redis" {
  name = "${var.environment}-${var.colour}-shop"

  subnet_ids = [
    "${data.terraform_remote_state.network.network-private-subnet-a-id}",
    "${data.terraform_remote_state.network.network-private-subnet-b-id}",
    "${data.terraform_remote_state.network.network-private-subnet-c-id}"
  ]
}

resource "aws_security_group" "redis" {
  name = "${var.environment}-${var.colour}-shop"

  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress = {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"

    cidr_blocks = [
      "${var.aws_network_vpc_cidr}",
      "${var.aws_bastion_vpc_cidr}"
    ]
  }

  tags {
    Environment = "${var.environment}"
    Colour      = "${var.colour}"
  }
}

resource "aws_route53_record" "redis-network" {
  zone_id = "${var.hosted_zone_id}"
  name    = "${var.environment}-${var.colour}-shop-redis.${var.hosted_zone_name}"
  type    = "CNAME"
  ttl     = "60"

  records = [
    "${aws_elasticache_cluster.redis.cache_nodes.0.address}"
  ]
}
