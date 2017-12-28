#!/bin/sh

cd $ROOT/terraform/rds

USERNAME=$(terraform output -json shop-username | jq -r '.value')
PASSWORD=$(terraform output -json shop-password | jq -r '.value')
ENDPOINT=$(terraform output -json shop-endpoint | jq -r '.value')

echo "provider \"mysql\" {\nendpoint = \"$ENDPOINT\"\nusername = \"$USERNAME\"\npassword = \"$PASSWORD\"\nversion = \"~> 0.1\"\n}" > $ROOT/setup/provider.tf

echo "Adding SSH key..."
mkdir ~/.ssh
ssh-keyscan $1 2> /dev/null >> ~/.ssh/known_hosts
echo "done."

echo "Copying files..."
scp -i $ROOT/deployer_key.pem $ROOT/setup/provider.tf ec2-user@$1:~
scp -i $ROOT/deployer_key.pem $ROOT/setup/main.tf ec2-user@$1:~
echo "done."

echo "Configuring databases..."
ssh -i $ROOT/deployer_key.pem ec2-user@$1 <<END
terraform init -input=false
terraform plan -input=false -out=tfplan
terraform apply -input=false tfplan
END
echo "done."
