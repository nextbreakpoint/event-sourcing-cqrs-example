docker rm -f some-mysql

docker run --name some-mysql -e MYSQL_ALLOW_EMPTY_PASSWORD=1 -p 3306:3306 -d mysql:latest

sleep 20 

docker exec -i some-mysql bash -c "mysql -u root" < setup.sql
