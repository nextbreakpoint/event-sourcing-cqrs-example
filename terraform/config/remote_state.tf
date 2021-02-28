##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "terraform"
    region = "eu-west-1"
    key    = "blueprint-config.tfstate"
  }
}

data "terraform_remote_state" "secrets" {
  backend = "s3"

  config {
    bucket = "terraform"
    region = "eu-west-1"
    key    = "env:/${terraform.workspace}/secrets.tfstate"
  }
}
