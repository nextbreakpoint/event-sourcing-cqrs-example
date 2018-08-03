##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "nextbreakpoint-terraform"
    region = "eu-west-1"
    key    = "shop-rds.tfstate"
  }
}

data "terraform_remote_state" "vpc" {
  backend = "s3"

  config {
    bucket = "nextbreakpoint-terraform"
    region = "eu-west-1"
    key    = "env:/${terraform.workspace}/vpc.tfstate"
  }
}

data "terraform_remote_state" "network" {
  backend = "s3"

  config {
    bucket = "nextbreakpoint-terraform"
    region = "eu-west-1"
    key    = "env:/${terraform.workspace}/network.tfstate"
  }
}
