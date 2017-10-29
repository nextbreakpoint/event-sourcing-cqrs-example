#!/usr/bin/env bash
set -e

sudo apt update -y
sudo apt-get install -y unzip

sudo curl -O https://releases.hashicorp.com/terraform/0.10.5/terraform_0.10.5_linux_amd64.zip

sudo unzip -d /tmp terraform_0.10.5_linux_amd64.zip

cd /tmp

./terraform init -input=false

./terraform plan -input=false -out=tfplan

./terraform apply -input=false tfplan
