#!/bin/bash

set -e

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable"
sudo add-apt-repository "deb [arch=amd64] https://packages.adoptium.net/artifactory/deb focal main"
sudo apt-get update -y && sudo apt-get install -y ca-certificates curl gnupg lsb-release temurin-17-jdk maven docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER && newgrp docker

sudo echo 127.0.0.1 host.docker.internal >> /etc/hosts
