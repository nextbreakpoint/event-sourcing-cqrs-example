#!/bin/sh

docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --delete --zookeeper zookeeper1:2181 --topic designs-events
docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --delete --zookeeper zookeeper1:2181 --topic designs-view
docker run --rm -it --net=services $KAFKA_IMAGE /opt/kafka_2.11-$KAFKA_VERSION/bin/kafka-topics.sh --delete --zookeeper zookeeper1:2181 --topic designs-sse
