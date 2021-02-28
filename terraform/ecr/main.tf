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
  name = "${var.environment}-${var.colour}-blueprint/designs-command"
}

resource "aws_ecr_repository" "designs-processor" {
  name = "${var.environment}-${var.colour}-blueprint/designs-processor"
}

resource "aws_ecr_repository" "designs-query" {
  name = "${var.environment}-${var.colour}-blueprint/designs-query"
}

resource "aws_ecr_repository" "designs-sse" {
  name = "${var.environment}-${var.colour}-blueprint/designs-sse"
}

resource "aws_ecr_repository" "designs" {
  name = "${var.environment}-${var.colour}-blueprint/designs"
}

resource "aws_ecr_repository" "accounts" {
  name = "${var.environment}-${var.colour}-blueprint/accounts"
}

resource "aws_ecr_repository" "authentication" {
  name = "${var.environment}-${var.colour}-blueprint/authentication"
}

resource "aws_ecr_repository" "gateway" {
  name = "${var.environment}-${var.colour}-blueprint/gateway"
}

resource "aws_ecr_repository" "weblet-root" {
  name = "${var.environment}-${var.colour}-blueprint/weblet-root"
}
