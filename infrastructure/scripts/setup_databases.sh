#!/bin/sh

. $ROOT/bash_aliases

HOSTED_ZONE_NAME=$(cat $ROOT/config/main.json | jq -r ".hosted_zone_name")
ENVIRONMENT=$(cat $ROOT/config/main.json | jq -r ".environment")
COLOUR=$(cat $ROOT/config/main.json | jq -r ".colour")
KEY_NAME=$(cat $ROOT/config/misc.json | jq -r ".key_name")

VERTICLE_USERNAME=$(cat $ROOT/config/main.json | jq -r ".mysql_verticle_username")
VERTICLE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".mysql_verticle_password")
LIQUIBASE_USERNAME=$(cat $ROOT/config/main.json | jq -r ".mysql_liquibase_username")
LIQUIBASE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".mysql_liquibase_password")

USERNAME=$(cd $ROOT/terraform/rds && terraform output -json shop-rds-username | jq -r '.value')
PASSWORD=$(cd $ROOT/terraform/rds && terraform output -json shop-rds-password | jq -r '.value')
HOSTNAME=$(cd $ROOT/terraform/rds && terraform output -json shop-rds-hostname | jq -r '.value')

echo "Adding SSH key..."
mkdir ~/.ssh
ssh-keyscan ${ENVIRONMENT}-${COLOUR}-bastion.${HOSTED_ZONE_NAME} 2> /dev/null >> ~/.ssh/known_hosts
echo "done."

echo "Copying files..."
scp -i $ROOT/${ENVIRONMENT}-${COLOUR}-${KEY_NAME}.pem $ROOT/terraform/mysql/mysql.tf ec2-user@${ENVIRONMENT}-${COLOUR}-bastion.${HOSTED_ZONE_NAME}:~
echo "done."

echo "Configuring databases..."
ssh -i $ROOT/${ENVIRONMENT}-${COLOUR}-${KEY_NAME}.pem ec2-user@${ENVIRONMENT}-${COLOUR}-bastion.${HOSTED_ZONE_NAME} <<END
echo "provider \"mysql\" { endpoint = \"${HOSTNAME}:3306\" username = \"${USERNAME}\" password = \"${PASSWORD}\" version = \"~> 0.1\"}" > provider.tf
cat provider.tf
terraform init -input=false -var mysql_verticle_username=$VERTICLE_USERNAME -var mysql_verticle_password=$VERTICLE_PASSWORD -var mysql_liquibase_username=$LIQUIBASE_USERNAME -var mysql_liquibase_password=$LIQUIBASE_PASSWORD
terraform plan -input=false -out=tfplan -var mysql_verticle_username=$VERTICLE_USERNAME -var mysql_verticle_password=$VERTICLE_PASSWORD -var mysql_liquibase_username=$LIQUIBASE_USERNAME -var mysql_liquibase_password=$LIQUIBASE_PASSWORD
terraform apply -input=false tfplan
END
echo "done."
