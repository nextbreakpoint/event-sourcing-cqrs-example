package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.HashMap;
import java.util.Map;

public class KafkaClientFactory {
    private KafkaClientFactory() {}

    public static <K, V> KafkaProducer<K, V> createProducer(Vertx vertx, JsonObject config) {
        final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");
        final String keySerializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringSerializer");
        final String valSerializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringSerializer");
        final String acks = config.getString("kafka_acks", "1");

        final Map<String, String> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valSerializer);
        producerConfig.put(ProducerConfig.ACKS_CONFIG, acks);

        return KafkaProducer.create(vertx, producerConfig);
    }

    public static <K, V> KafkaConsumer<K, V> createConsumer(Vertx vertx, JsonObject config) {
        final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");
        final String keyDeserializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringDeserializer");
        final String valDeserializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringDeserializer");
        final String groupId = config.getString("kafka_group_id", "test");
        final String autoOffsetReset = config.getString("kafka_auto_offset_reset", "earliest");
        final String enableAutoCommit = config.getString("kafka_enable_auto_commit", "false");

        final Map<String, String> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valDeserializer);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);

        return KafkaConsumer.create(vertx, consumerConfig);
    }
}
