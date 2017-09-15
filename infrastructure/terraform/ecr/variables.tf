###################################################################
# AWS configuration below
###################################################################

variable "aws_region" {
  default = "eu-west-1"
}

variable "aws_profile" {
  default = "default"
}

### MANDATORY ###
variable "public_hosted_zone_id" {
}

### MANDATORY ###
variable "public_hosted_zone_name" {
}
