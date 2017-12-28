#!/bin/sh

kubectl create secret generic mysql-scripts --from-file=mysql-create-databases.sql --from-file=mysql-create-users.sql
