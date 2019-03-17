##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "nextbreakpoint-terraform"
    region = "eu-west-1"
    key    = "shop-config.tfstate"
  }
}

data "terraform_remote_state" "secrets" {
  backend = "s3"

  config {
    bucket = "nextbreakpoint-terraform"
    region = "eu-west-1"
    key    = "env:/${terraform.workspace}/secrets.tfstate"
  }
}
