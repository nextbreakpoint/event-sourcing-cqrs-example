#!/bin/sh

kubectl exec -it mysql-server-$1 -- /bin/bash -c "mysql -u root -p < /scripts/mysql-create-databases.sql"
kubectl exec -it mysql-server-$1 -- /bin/bash -c "mysql -u root -p < /scripts/mysql-create-users.sql"
