docker run -d mysql:latest --name mysql --net=bridge --restart=always -p 3306 -e MYSQL_ROOT_PASSWORD=password

docker run -d --name graphite --net=bridge --restart=always -p 80 -p 2003-2004 -p 2023-2024 -p 8125/udp -p 8126 hopsoft/graphite-statsd

docker run -d --name=grafana --net=bridge --restart=always -p 3000 grafana/grafana

#kubectl run mysql --image=mysql:latest --port=3306 --env="MYSQL_ROOT_PASSWORD=password"
#kubectl expose deployment mysql
#kubectl run grafana --image=grafana/grafana --port=3000
#kubectl expose deployment grafana
#kubectl run graphite --image=hopsoft/graphite-statsd
#kubectl expose deployment graphite

