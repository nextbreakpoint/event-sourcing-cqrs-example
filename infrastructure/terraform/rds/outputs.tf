output "rds-services-username" {
  value = "${aws_db_instance.services.username}"
}

output "rds-services-password" {
  value = "${aws_db_instance.services.password}"
}
