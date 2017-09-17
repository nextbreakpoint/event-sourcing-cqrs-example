resource "mysql_database" "accounts" {
  name = "accounts"
}

resource "mysql_database" "designs" {
  name = "designs"
}

resource "mysql_user" "verticle" {
  user     = "verticle"
  password = "password"
  host     = "%"
}

resource "mysql_user" "liquibase" {
  user     = "liquibase"
  password = "password"
  host     = "%"
}

resource "mysql_grant" "verticle_accounts" {
  user       = "${mysql_user.verticle.user}"
  host       = "${mysql_user.verticle.host}"
  database   = "${mysql_database.accounts.name}"
  privileges = ["SELECT", "UPDATE", "INSERT", "DELETE"]
}

resource "mysql_grant" "liquibase_accounts" {
  user       = "${mysql_user.liquibase.user}"
  host       = "${mysql_user.liquibase.host}"
  database   = "${mysql_database.accounts.name}"
  privileges = ["ALL"]
}

resource "mysql_grant" "verticle_designs" {
  user       = "${mysql_user.verticle.user}"
  host       = "${mysql_user.verticle.host}"
  database   = "${mysql_database.designs.name}"
  privileges = ["SELECT", "UPDATE", "INSERT", "DELETE"]
}

resource "mysql_grant" "liquibase_designs" {
  user       = "${mysql_user.liquibase.user}"
  host       = "${mysql_user.liquibase.host}"
  database   = "${mysql_database.designs.name}"
  privileges = ["ALL"]
}
