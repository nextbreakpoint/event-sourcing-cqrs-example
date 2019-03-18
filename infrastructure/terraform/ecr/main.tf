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

resource "aws_ecr_repository" "authentication" {
  name = "${var.environment}-${var.colour}-shop/authentication"
}

resource "aws_ecr_repository" "gateway" {
  name = "${var.environment}-${var.colour}-shop/gateway"
}

resource "aws_ecr_repository" "weblet-admin" {
  name = "${var.environment}-${var.colour}-shop/weblet-admin"
}

resource "aws_ecr_repository" "weblet-static" {
  name = "${var.environment}-${var.colour}-shop/weblet-static"
}
