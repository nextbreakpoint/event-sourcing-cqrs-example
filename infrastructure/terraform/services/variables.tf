###################################################################
# AWS configuration below
###################################################################

variable "aws_region" {
  default = "eu-west-1"
}

variable "aws_profile" {
  default = "default"
}

variable "stream_tag" {
  default = "terraform"
}

### MANDATORY ###
variable "account_id" {
}

### MANDATORY ###
variable "secrets_bucket_name" {
}
