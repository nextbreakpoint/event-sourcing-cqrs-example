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
