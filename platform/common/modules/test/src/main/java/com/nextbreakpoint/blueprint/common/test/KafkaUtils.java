package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Environment;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

import java.util.HashMap;
import java.util.Map;

public class KafkaUtils {
    private KafkaUtils() {}

    public static <K, V> KafkaProducer<K, V> createProducer(Environment environment, JsonObject config) {
        final String bootstrapServers = environment.resolve(config.getString("kafka_bootstrap_servers", "localhost:9092"));
        final String keySerializer = environment.resolve(config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringSerializer"));
        final String valSerializer = environment.resolve(config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringSerializer"));
        final String acks = environment.resolve(config.getString("kafka_acks", "1"));

        final String keystoreLocation = environment.resolve(config.getString("kafka_keystore_location"));
        final String keystorePassword = environment.resolve(config.getString("kafka_keystore_password"));
        final String truststoreLocation = environment.resolve(config.getString("kafka_truststore_location"));
        final String truststorePassword = environment.resolve(config.getString("kafka_truststore_password"));

        final Map<String, String> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valSerializer);
        producerConfig.put(ProducerConfig.ACKS_CONFIG, acks);

        if (keystoreLocation != null && truststoreLocation != null) {
            producerConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            producerConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
            if (keystorePassword != null) {
                producerConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
            }

            producerConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
            if (truststorePassword != null) {
                producerConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
            }
        }

        return new KafkaProducer(producerConfig);
    }

    public static <K, V> KafkaConsumer<K, V> createConsumer(Environment environment, JsonObject config) {
        final String bootstrapServers = environment.resolve(config.getString("kafka_bootstrap_servers", "localhost:9092"));
        final String keyDeserializer = environment.resolve(config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        final String valDeserializer = environment.resolve(config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        final String groupId = environment.resolve(config.getString("kafka_group_id", "test"));
        final String autoOffsetReset = environment.resolve(config.getString("kafka_auto_offset_reset", "earliest"));
        final String enableAutoCommit = environment.resolve(config.getString("kafka_enable_auto_commit", "false"));

        final String keystoreLocation = environment.resolve(config.getString("kafka_keystore_location"));
        final String keystorePassword = environment.resolve(config.getString("kafka_keystore_password"));
        final String truststoreLocation = environment.resolve(config.getString("kafka_truststore_location"));
        final String truststorePassword = environment.resolve(config.getString("kafka_truststore_password"));

        final Map<String, String> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valDeserializer);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);

        if (keystoreLocation != null && truststoreLocation != null) {
            consumerConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            consumerConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
            if (keystorePassword != null) {
                consumerConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
            }

            consumerConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
            if (truststorePassword != null) {
                consumerConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
            }
        }

        return new KafkaConsumer(consumerConfig);
    }
}
