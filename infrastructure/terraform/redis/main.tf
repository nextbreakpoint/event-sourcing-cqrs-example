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
    key = "services-redis.tfstate"
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

##############################################################################
# Redis
##############################################################################

resource "aws_elasticache_cluster" "redis" {
  cluster_id = "services"
  engine = "redis"
  engine_version = "3.2.4"
  node_type = "cache.t2.medium"
  port = 6379
  num_cache_nodes = 1
  parameter_group_name = "default.redis3.2"
  subnet_group_name = "${aws_elasticache_subnet_group.redis.name}"
  security_group_ids = ["${aws_security_group.redis.id}"]
}

resource "aws_elasticache_subnet_group" "redis" {
  name = "redis-subnet-group"

  subnet_ids = [
    "${data.terraform_remote_state.vpc.network-private-subnet-a-id}",
    "${data.terraform_remote_state.vpc.network-private-subnet-b-id}"
  ]
}

resource "aws_security_group" "redis" {
  name = "redis-security-group"

  vpc_id = "${data.terraform_remote_state.vpc.network-vpc-id}"

  ingress = {
    from_port = 6379
    to_port = 6379
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

##############################################################################
# Route 53
##############################################################################

resource "aws_route53_record" "redis" {
  zone_id = "${data.terraform_remote_state.vpc.hosted-zone-id}"
  name = "redis-services.${var.hosted_zone_name}"
  type = "CNAME"
  ttl = "30"

  records = [
    "${aws_elasticache_cluster.redis.cache_nodes.0.address}"
  ]
}
