###################################################################
# AWS configuration below
###################################################################

variable "aws_region" {
  default = "eu-west-1"
}

variable "aws_profile" {
  default = "default"
}

##############################################################################
# Resources configuration below
##############################################################################

### MANDATORY ###
variable "environment" {}

### MANDATORY ###
variable "colour" {}

### MANDATORY ###
variable "account_id" {}

### MANDATORY ###
variable "secrets_bucket_name" {}

### MANDATORY ###
variable "hosted_zone_name" {}

### MANDATORY ###
variable "github_client_id" {}

### MANDATORY ###
variable "github_client_secret" {}

### MANDATORY ###
variable "github_user_email" {}

### MANDATORY ###
variable "mysql_verticle_username" {}

### MANDATORY ###
variable "mysql_verticle_password" {}

### MANDATORY ###
variable "mysql_liquibase_username" {}

### MANDATORY ###
variable "mysql_liquibase_password" {}
