package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

import java.util.HashMap;
import java.util.Map;

public class KafkaClientFactory {
    private KafkaClientFactory() {}

    public static <K, V> KafkaProducer<K, V> createProducer(Vertx vertx, KafkaProducerConfig producerConfig) {
        final Map<String, String> kafkaProducerConfig = new HashMap<>();

        kafkaProducerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBootstrapServers());
        kafkaProducerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfig.getKeySerializer());
        kafkaProducerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfig.getValueSerializer());
        kafkaProducerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, producerConfig.getClientId());
        kafkaProducerConfig.put(ProducerConfig.ACKS_CONFIG, producerConfig.getKafkaAcks());

        if (producerConfig.getKeystoreLocation() != null && producerConfig.getTruststoreLocation() != null) {
            kafkaProducerConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            kafkaProducerConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, producerConfig.getKeystoreLocation());
            if (producerConfig.getKeystorePassword() != null) {
                kafkaProducerConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, producerConfig.getKeystorePassword());
            }

            kafkaProducerConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, producerConfig.getTruststoreLocation());
            if (producerConfig.getTruststorePassword() != null) {
                kafkaProducerConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, producerConfig.getTruststorePassword());
            }
        }

        return KafkaProducer.create(vertx, kafkaProducerConfig);
    }

    public static <K, V> KafkaConsumer<K, V> createConsumer(Vertx vertx, KafkaConsumerConfig consumerConfig) {
        final Map<String, String> kafkaConsumerConfig = new HashMap<>();

        kafkaConsumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerConfig.getBootstrapServers());
        kafkaConsumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerConfig.getKeyDeserializer());
        kafkaConsumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerConfig.getValueDeserializer());
        kafkaConsumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConfig.getGroupId());
        kafkaConsumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getAutoOffsetReset());
        kafkaConsumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfig.getEnableAutoCommit());

        if (consumerConfig.getKeystoreLocation() != null && consumerConfig.getTruststoreLocation() != null) {
            kafkaConsumerConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            kafkaConsumerConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, consumerConfig.getKeystoreLocation());
            if (consumerConfig.getKeystorePassword() != null) {
                kafkaConsumerConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, consumerConfig.getKeystorePassword());
            }

            kafkaConsumerConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, consumerConfig.getTruststoreLocation());
            if (consumerConfig.getTruststorePassword() != null) {
                kafkaConsumerConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, consumerConfig.getTruststorePassword());
            }
        }

        return KafkaConsumer.create(vertx, kafkaConsumerConfig);
    }
}
