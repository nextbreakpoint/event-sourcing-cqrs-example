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

resource "aws_ecr_repository" "designs-command" {
  name = "${var.environment}-${var.colour}-shop/designs-command"
}

resource "aws_ecr_repository" "designs-processor" {
  name = "${var.environment}-${var.colour}-shop/designs-processor"
}

resource "aws_ecr_repository" "designs-query" {
  name = "${var.environment}-${var.colour}-shop/designs-query"
}

resource "aws_ecr_repository" "designs-sse" {
  name = "${var.environment}-${var.colour}-shop/designs-sse"
}

resource "aws_ecr_repository" "designs" {
  name = "${var.environment}-${var.colour}-shop/designs"
}

resource "aws_ecr_repository" "accounts" {
  name = "${var.environment}-${var.colour}-shop/accounts"
}

resource "aws_ecr_repository" "auth" {
  name = "${var.environment}-${var.colour}-shop/auth"
}

resource "aws_ecr_repository" "web" {
  name = "${var.environment}-${var.colour}-shop/web"
}

resource "aws_ecr_repository" "api-gateway" {
  name = "${var.environment}-${var.colour}-shop/api-gateway"
}
