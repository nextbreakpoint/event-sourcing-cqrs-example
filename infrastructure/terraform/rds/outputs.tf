###################################################################
# Outputs
###################################################################

output "shop-username" {
  value = "${aws_db_instance.shop.username}"
}

output "shop-password" {
  value = "${aws_db_instance.shop.password}"
}

output "shop-endpoint" {
  value = "shop-rds.${var.hosted_zone_name}:3306"
}
