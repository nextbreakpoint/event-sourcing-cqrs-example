docker rm -f some-mysql

docker run --name some-mysql -e MYSQL_ALLOW_EMPTY_PASSWORD=1 -p 3306:3306 -d --net=development --net-alias=database mysql:latest

sleep 20

docker exec -i some-mysql bash -c "mysql -u root" < scripts/setup.sql

docker rm -f some-nginx

docker run --name some-nginx -v $(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro -v $(pwd)/nginx:/nginx:ro -d --net=development --privileged -p 80:80 -p 443:443 nginx

