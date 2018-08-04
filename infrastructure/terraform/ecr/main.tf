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

resource "aws_ecr_repository" "accounts" {
  name = "${var.environment}-${var.colour}-shop/accounts"
}

resource "aws_ecr_repository" "designs" {
  name = "${var.environment}-${var.colour}-shop/designs"
}

resource "aws_ecr_repository" "auth" {
  name = "${var.environment}-${var.colour}-shop/auth"
}

resource "aws_ecr_repository" "web" {
  name = "${var.environment}-${var.colour}-shop/web"
}
