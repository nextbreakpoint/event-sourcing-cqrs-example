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
# ECR
##############################################################################

resource "aws_ecr_repository" "accounts" {
  name = "services/accounts"
}

resource "aws_ecr_repository" "designs" {
  name = "services/designs"
}

resource "aws_ecr_repository" "auth" {
  name = "services/auth"
}

resource "aws_ecr_repository" "web" {
  name = "services/web"
}

##############################################################################
# Route 53
##############################################################################

resource "aws_route53_record" "ecr" {
  zone_id = "${var.public_hosted_zone_id}"
  name = "registry.${var.public_hosted_zone_name}"
  type = "CNAME"
  ttl = "300"

  records = [
    "${var.account_id}.dkr.ecr.region.amazonaws.com"
  ]
}
