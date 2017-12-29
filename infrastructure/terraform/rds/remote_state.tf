##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "terraform"
    region = "eu-west-1"
    key = "shop-rds.tfstate"
  }
}

data "terraform_remote_state" "vpc" {
    backend = "s3"
    config {
        bucket = "terraform"
        region = "eu-west-1"
        key = "vpc.tfstate"
    }
}

data "terraform_remote_state" "zones" {
    backend = "s3"
    config {
        bucket = "terraform"
        region = "eu-west-1"
        key = "zones.tfstate"
    }
}

data "terraform_remote_state" "network" {
    backend = "s3"
    config {
        bucket = "terraform"
        region = "eu-west-1"
        key = "network.tfstate"
    }
}
