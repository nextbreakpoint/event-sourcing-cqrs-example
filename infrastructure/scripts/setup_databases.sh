#!/bin/sh

. $ROOT/bash_aliases

cd $ROOT/terraform/rds

ENVIRONMENT=$(cat $ROOT/config/main.json | jq -r ".environment")
COLOUR=$(cat $ROOT/config/main.json | jq -r ".colour")
KEY_NAME=$(cat $ROOT/config/misc.json | jq -r ".key_name")

VERTICLE_USERNAME=$(cat $ROOT/config/main.json | jq -r ".mysql_verticle_username")
VERTICLE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".mysql_verticle_password")

LIQUIBASE_USERNAME=$(cat $ROOT/config/main.json | jq -r ".mysql_liquibase_username")
LIQUIBASE_PASSWORD=$(cat $ROOT/config/main.json | jq -r ".mysql_liquibase_password")

USERNAME=$(terraform output -json shop-username | jq -r '.value')
PASSWORD=$(terraform output -json shop-password | jq -r '.value')
ENDPOINT=$(terraform output -json shop-endpoint | jq -r '.value')

echo "Adding SSH key..."
mkdir ~/.ssh
ssh-keyscan $1 2> /dev/null >> ~/.ssh/known_hosts
echo "done."

echo "Copying files..."
scp -i $ROOT/${ENVIRONMENT}-${COLOUR}-${KEY_NAME}.pem $ROOT/terraform/mysql/mysql.tf ec2-user@$1:~
echo "done."

echo "Configuring databases..."
ssh -i $ROOT/${ENVIRONMENT}-${COLOUR}-${KEY_NAME}.pem ec2-user@$1 <<END
echo "provider \"mysql\" {\nendpoint = \"$ENDPOINT\"\nusername = \"$USERNAME\"\npassword = \"$PASSWORD\"\nversion = \"~> 0.1\"\n}" > provider.tf
terraform init -input=false -var mysql_verticle_username=$VERTICLE_USERNAME -var mysql_verticle_password=$VERTICLE_PASSWORD -var mysql_liquibase_username=$LIQUIBASE_USERNAME -var mysql_liquibase_password=$LIQUIBASE_PASSWORD
terraform plan -input=false -out=tfplan -var mysql_verticle_username=$VERTICLE_USERNAME -var mysql_verticle_password=$VERTICLE_PASSWORD -var mysql_liquibase_username=$LIQUIBASE_USERNAME -var mysql_liquibase_password=$LIQUIBASE_PASSWORD
terraform apply -input=false tfplan
END
echo "done."
