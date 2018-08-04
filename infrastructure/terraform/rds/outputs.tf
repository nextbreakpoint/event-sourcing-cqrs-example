###################################################################
# Outputs
###################################################################

output "shop-rds-username" {
  value = "${aws_db_instance.shop.username}"
}

output "shop-rds-password" {
  value = "${aws_db_instance.shop.password}"
}

output "shop-rds-hostname" {
  value = "${var.environment}-${var.colour}-shop-rds.${var.hosted_zone_name}"
}
