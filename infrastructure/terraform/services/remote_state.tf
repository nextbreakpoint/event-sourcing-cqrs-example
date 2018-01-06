##############################################################################
# Remote state
##############################################################################

terraform {
  backend "s3" {
    bucket = "terraform"
    region = "eu-west-1"
    key    = "shop-ecs.tfstate"
  }
}

data "terraform_remote_state" "ecs" {
  backend = "s3"

  config {
    bucket = "terraform"
    region = "eu-west-1"
    key    = "ecs.tfstate"
  }
}
