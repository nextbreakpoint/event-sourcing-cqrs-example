#!/bin/sh

docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-events --replication-factor 3 --partitions 10
docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-view --replication-factor 3 --partitions 10
docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --create --zookeeper zookeeper1:2181 --topic designs-sse --replication-factor 3 --partitions 10
