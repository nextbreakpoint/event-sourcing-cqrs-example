output "shop-username" {
  value = "${aws_db_instance.shop.username}"
}

output "shop-password" {
  value = "${aws_db_instance.shop.password}"
}

output "shop-endpoint" {
  value = "shop-rds.${data.terraform_remote_state.vpc.bastion-hosted-zone-name}:3306"
}
