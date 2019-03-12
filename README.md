# example-images-shop

This repository contains the source code and the deployment scripts of an example of micro-services based application. The application requires the infrastructure documented in the repository [infrastructure-as-code](https://github.com/nextbreakpoint/infrastructure-as-code). The micro-services are written in Java using [Vert.x](https://vertx.io) framework and they depend on Apache Cassandra, Apache Kafka and Apache Zookeeper.

    THIS PROJECT IS WORK IN PROGRESS

## Configure and generate secrets

Create a file main.json in the infrastructure/config directory. Copy the content from the file template-main.json. The file should look like:

    {
      "account_id": "your_account_id",

      "environment": "prod",
      "colour": "green",

      "hosted_zone_name": "yourdomain.com",
      "hosted_zone_id": "your_public_zone_id",

      "bastion_host": "bastion.yourdomain.com",

      "secrets_bucket_name": "your_secrets_bucket_name",

      "github_user_email": "your_github_user_email",
      "github_client_id": "your_github_client_id",
      "github_client_secret": "your_github_client_secret",

      "key_password": "your_key_password",
      "keystore_password": "your_keystore_password",
      "truststore_password": "your_truststore_password",

      "cassandra_username": "cassandra",
      "cassandra_password": "cassandra",

      "mysql_username": "shop",
      "mysql_password": "changeme"
    }

Configure Terraform's backend with command:

    ./docker_run.sh configure_terraform your_terraform_bucket

Generate secrets with command:

    ./docker_run.sh generate_secrets

## Deploy the application

Create ECR repositories with command:

    ./docker_run.sh module_create ecr

Build Docker images and push images to ECR with command:

    ./build_and_push_images.sh your_aws_account_id.dkr.ecr.eu-west-1.amazonaws.com your_access_id your_secret_access_key

Create services configuration files with command:

    ./docker_run.sh module_create services

Create NGINX configuration file with command:

    ./docker_run.sh module_create nginx

Create secrets files in S3 with command:

    ./docker_run.sh module_create secrets

Create target groups and Route53's records with command:

    ./docker_run.sh module_create targets

In order to create the targets groups you must create the Load Balancers (see [infrastructure-as-code](https://github.com/nextbreakpoint/infrastructure-as-code)).

Copy SSH key for accessing EC2 machines and change permissions:

    chmod 600 prod-green-deployer.pem

Create Cassandra keyspaces and users with command:

    ./cassandra_script.sh create

Deploy NGINX server on Docker Swarm with command:

    ./swarm_run.sh deploy_stack shop-nginx

Deploy MySQL server on Docker Swarm with command:

    ./swarm_run.sh deploy_stack shop-mysql

Configure MySQL server with command:

    ./swarm_run.sh setup_mysql

Configure Kafka topics with command:

    ./swarm_run.sh create_kafka_topics

Deploy services on Docker Swarm with commands:

    ./swarm_run.sh deploy_stack shop-auth
    ./swarm_run.sh deploy_stack shop-accounts
    ./swarm_run.sh deploy_stack shop-designs-command
    ./swarm_run.sh deploy_stack shop-designs-processor
    ./swarm_run.sh deploy_stack shop-designs-query
    ./swarm_run.sh deploy_stack shop-designs-sse
    ./swarm_run.sh deploy_stack shop-web

## Access the application

Open a browser at https://prod-green-shop.yourdomain.com/content/designs.

You will be redirected to GitHub for authentication. Use the email you configured in the main.json to login as admin.

## Destroy the application

Remove services with commands:

    ./swarm_run.sh remove_stack shop-auth
    ./swarm_run.sh remove_stack shop-accounts
    ./swarm_run.sh remove_stack shop-designs-command
    ./swarm_run.sh remove_stack shop-designs-processor
    ./swarm_run.sh remove_stack shop-designs-query
    ./swarm_run.sh remove_stack shop-designs-sse
    ./swarm_run.sh remove_stack shop-web

Remove MySQL server with command:

    ./swarm_run.sh remove_stack shop-mysql

Remove NGINX server with command:

    ./swarm_run.sh remove_stack shop-nginx

Delete Kafka topics with command:

    ./swarm_run.sh delete_kafka_topics

Delete Cassandra keyspaces and users with command:

    ./cassandra_script.sh destroy

Destroy target groups and Route53's records with command:

    ./docker_run.sh module_destroy targets

Destroy secrets files in S3 with command:

    ./docker_run.sh module_destroy secrets

Destroy ECR repositories with command:

    ./docker_run.sh module_destroy ecr

## Reset Terraform

Reset Terraform's state with command:

    ./docker_run.sh reset_terraform

Be careful to don't reset the state before destroying the infrastructure.
