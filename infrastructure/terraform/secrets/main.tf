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

resource "aws_s3_bucket_object" "keystore-auth" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/keystores/keystore-auth.jceks"
  source = "../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-auth.jceks"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-auth.jceks"))}"
}

resource "aws_s3_bucket_object" "keystore-client" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/keystores/keystore-client.jks"
  source = "../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-client.jks"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-client.jks"))}"
}

resource "aws_s3_bucket_object" "keystore-server" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/keystores/keystore-server.jks"
  source = "../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-server.jks"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/keystores/keystore-server.jks"))}"
}

resource "aws_s3_bucket_object" "truststore-client" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/keystores/truststore-client.jks"
  source = "../../secrets/environments/${var.environment}/${var.colour}/keystores/truststore-client.jks"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/keystores/truststore-client.jks"))}"
}

resource "aws_s3_bucket_object" "truststore-server" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/keystores/truststore-server.jks"
  source = "../../secrets/environments/${var.environment}/${var.colour}/keystores/truststore-server.jks"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/keystores/truststore-server.jks"))}"
}

resource "aws_s3_bucket_object" "auth" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/config/auth.json"
  source = "../../secrets/environments/${var.environment}/${var.colour}/config/auth.json"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/config/auth.json"))}"
}

resource "aws_s3_bucket_object" "designs" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/config/designs.json"
  source = "../../secrets/environments/${var.environment}/${var.colour}/config/designs.json"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/config/designs.json"))}"
}

resource "aws_s3_bucket_object" "accounts" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/config/accounts.json"
  source = "../../secrets/environments/${var.environment}/${var.colour}/config/accounts.json"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/config/accounts.json"))}"
}

resource "aws_s3_bucket_object" "web" {
  bucket = "${var.secrets_bucket_name}"
  key    = "environments/${var.environment}/${var.colour}/shop/config/web.json"
  source = "../../secrets/environments/${var.environment}/${var.colour}/config/web.json"
  etag   = "${md5(file("../../secrets/environments/${var.environment}/${var.colour}/config/web.json"))}"
}
