#!/bin/sh

CONTAINER_ID=$(docker ps --filter name=kafka -q)

docker exec -it -e KAFKA_OPTS="-Djava.security.auth.login.config=/secrets/zookeeper_kafka_jaas.conf" $CONTAINER_ID /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-events --replication-factor 3 --partitions 10
docker exec -it -e KAFKA_OPTS="-Djava.security.auth.login.config=/secrets/zookeeper_kafka_jaas.conf" $CONTAINER_ID /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-view --replication-factor 3 --partitions 10
docker exec -it -e KAFKA_OPTS="-Djava.security.auth.login.config=/secrets/zookeeper_kafka_jaas.conf" $CONTAINER_ID /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-sse --replication-factor 3 --partitions 10
