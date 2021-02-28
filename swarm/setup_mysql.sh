#!/bin/sh

  docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"DROP DATABASE IF EXISTS blueprint;\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"CREATE DATABASE blueprint CHARACTER SET utf8 COLLATE utf8_bin;\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"CREATE USER IF NOT EXISTS 'blueprint' IDENTIFIED WITH mysql_native_password BY 'password' PASSWORD EXPIRE NEVER;\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"FLUSH PRIVILEGES;\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"GRANT ALL ON blueprint.* TO 'blueprint'@'%';\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"FLUSH PRIVILEGES;\""

docker run --rm -it --net=services mysql:5.7 sh -c "mysql -h blueprint-mysql -e \"USE blueprint; CREATE TABLE IF NOT EXISTS ACCOUNTS (UUID VARCHAR(36) PRIMARY KEY, NAME VARCHAR(1024) NOT NULL, EMAIL VARCHAR(1024) NOT NULL, ROLE VARCHAR(128) NOT NULL);\""

#docker run --rm -it --net=services mysql:5.7 sh -c "mysqladmin -u root password 'password'"
