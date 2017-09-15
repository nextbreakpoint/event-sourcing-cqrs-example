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
    key = "services-ecr.tfstate"
  }
}

##############################################################################
# ECR
##############################################################################

resource "aws_ecr_repository" "accounts-service" {
  name = "docker/accounts-service"
}

resource "aws_ecr_repository" "designs-service" {
  name = "docker/designs-service"
}

resource "aws_ecr_repository" "auth-service" {
  name = "docker/auth-service"
}

resource "aws_ecr_repository" "web-service" {
  name = "docker/web-service"
}

##############################################################################
# Route 53
##############################################################################

resource "aws_route53_record" "ecr_dns" {
  zone_id = "${var.public_hosted_zone_id}"
  name = "registry.${var.public_hosted_zone_name}"
  type = "CNAME"
  ttl = "300"

  records = [
    "${var.aws_account_id}.dkr.ecr.region.amazonaws.com"
  ]
}
