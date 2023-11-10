package com.nextbreakpoint.blueprint.common.drivers;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

import java.util.HashMap;
import java.util.Map;

public class KafkaClientFactory {
    private static final String SCHEMA_REGISTRY = "schema.registry";

    private KafkaClientFactory() {}

    public static <K, V> KafkaProducer<K, V> createProducer(KafkaProducerConfig producerConfig) {
        Map<String, Object> options = new HashMap<>();

        options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBootstrapServers());
        options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfig.getKeySerializer());
        options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfig.getValueSerializer());
        options.put(ProducerConfig.CLIENT_ID_CONFIG, producerConfig.getClientId());
        options.put(ProducerConfig.ACKS_CONFIG, producerConfig.getKafkaAcks());

        if (producerConfig.getSchemaRegistryUrl() != null) {
            options.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, producerConfig.getAutoRegisterSchemas());
            options.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, producerConfig.getSchemaRegistryUrl());
        }

        if (producerConfig.getKeystoreLocation() != null && producerConfig.getTruststoreLocation() != null) {
            options.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            options.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, producerConfig.getKeystoreLocation());
            if (producerConfig.getKeystorePassword() != null) {
                options.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, producerConfig.getKeystorePassword());
            }

            options.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, producerConfig.getTruststoreLocation());
            if (producerConfig.getTruststorePassword() != null) {
                options.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, producerConfig.getTruststorePassword());
            }
        }

        if (producerConfig.getSchemaRegistryKeystoreLocation() != null && producerConfig.getSchemaRegistryTruststoreLocation() != null) {
            options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, producerConfig.getSchemaRegistryKeystoreLocation());
            if (producerConfig.getSchemaRegistryKeystorePassword() != null) {
                options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, producerConfig.getSchemaRegistryKeystorePassword());
            }

            options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, producerConfig.getSchemaRegistryTruststoreLocation());
            if (producerConfig.getSchemaRegistryTruststorePassword() != null) {
                options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, producerConfig.getSchemaRegistryTruststorePassword());
            }
        }

        return new KafkaProducer<>(options);
    }

    public static <K, V> KafkaConsumer<K, V> createConsumer(KafkaConsumerConfig consumerConfig) {
        Map<String, Object> options = new HashMap<>();

        options.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerConfig.getBootstrapServers());
        options.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerConfig.getKeyDeserializer());
        options.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerConfig.getValueDeserializer());
        options.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConfig.getGroupId());
        options.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getAutoOffsetReset());
        options.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfig.getEnableAutoCommit());
        options.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");
        options.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");

        if (consumerConfig.getSchemaRegistryUrl() != null) {
            options.put(KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS, consumerConfig.getAutoRegisterSchemas());
            options.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, consumerConfig.getSchemaRegistryUrl());
            options.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, consumerConfig.getSpecificAvroReader());
        }

        if (consumerConfig.getKeystoreLocation() != null && consumerConfig.getTruststoreLocation() != null) {
            options.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            options.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, consumerConfig.getKeystoreLocation());
            if (consumerConfig.getKeystorePassword() != null) {
                options.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, consumerConfig.getKeystorePassword());
            }

            options.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, consumerConfig.getTruststoreLocation());
            if (consumerConfig.getTruststorePassword() != null) {
                options.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, consumerConfig.getTruststorePassword());
            }
        }

        if (consumerConfig.getSchemaRegistryKeystoreLocation() != null && consumerConfig.getSchemaRegistryTruststoreLocation() != null) {
            options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, consumerConfig.getSchemaRegistryKeystoreLocation());
            if (consumerConfig.getSchemaRegistryKeystorePassword() != null) {
                options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, consumerConfig.getSchemaRegistryKeystorePassword());
            }

            options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, consumerConfig.getSchemaRegistryTruststoreLocation());
            if (consumerConfig.getSchemaRegistryTruststorePassword() != null) {
                options.put(SCHEMA_REGISTRY + "." + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, consumerConfig.getSchemaRegistryTruststorePassword());
            }
        }

        return new KafkaConsumer<>(options);
    }
}
