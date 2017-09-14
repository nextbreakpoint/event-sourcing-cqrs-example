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
