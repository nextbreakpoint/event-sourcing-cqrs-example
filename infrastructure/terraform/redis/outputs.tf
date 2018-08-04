###################################################################
# Outputs
###################################################################

output "shop-redis-hostname" {
  value = "${var.environment}-${var.colour}-shop-redis.${var.hosted_zone_name}"
}
