# example-images-shop

THIS PROJECT IS WORK IN PROGRESS

This repository contains source code and deployment scripts of a micro-services based example application.

This application depends on the cloud-based infrastructure available here:

https://github.com/nextbreakpoint/infrastructure-as-code

./docker_run.sh configure_terraform your_terraform_bucket

./docker_run.sh generate_secrets

./docker_run.sh module_create ecr

./scripts/deploy.sh your_account.dkr.ecr.eu-west-1.amazonaws.com your_access_id your_secret_access_key

./docker_run.sh module_create services
./docker_run.sh module_create secrets
./docker_run.sh module_create rds
./docker_run.sh module_create redis
./docker_run.sh module_create targets

chmod 600 prod-green-deployer.pem

./docker_run.sh setup_databases

./swarm_run.sh deploy_stack shop-auth
./swarm_run.sh deploy_stack shop-accounts
./swarm_run.sh deploy_stack shop-designs
./swarm_run.sh deploy_stack shop-web

./docker_run.sh reset_terraform
