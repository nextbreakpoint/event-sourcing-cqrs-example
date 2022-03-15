package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.tracing.TracingPolicy;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerImpl;
import io.vertx.kafka.client.producer.KafkaWriteStream;
import io.vertx.kafka.client.producer.impl.KafkaProducerImpl;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

public class KafkaClientFactory {
    private KafkaClientFactory() {}

    public static <K, V> KafkaProducer<K, V> createProducer(Vertx vertx, KafkaProducerConfig producerConfig) {
        KafkaClientOptions options = new KafkaClientOptions();

        options.setTracingPolicy(TracingPolicy.IGNORE);

        options.setConfig(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBootstrapServers());
        options.setConfig(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfig.getKeySerializer());
        options.setConfig(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfig.getValueSerializer());
        options.setConfig(ProducerConfig.CLIENT_ID_CONFIG, producerConfig.getClientId());
        options.setConfig(ProducerConfig.ACKS_CONFIG, producerConfig.getKafkaAcks());

        if (producerConfig.getKeystoreLocation() != null && producerConfig.getTruststoreLocation() != null) {
            options.setConfig(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            options.setConfig(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, producerConfig.getKeystoreLocation());
            if (producerConfig.getKeystorePassword() != null) {
                options.setConfig(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, producerConfig.getKeystorePassword());
            }

            options.setConfig(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, producerConfig.getTruststoreLocation());
            if (producerConfig.getTruststorePassword() != null) {
                options.setConfig(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, producerConfig.getTruststorePassword());
            }
        }

        return KafkaClientFactory.createProducer(vertx, options);
    }

    public static <K, V> KafkaConsumer<K, V> createConsumer(Vertx vertx, KafkaConsumerConfig consumerConfig) {
        KafkaClientOptions options = new KafkaClientOptions();

        options.setTracingPolicy(TracingPolicy.IGNORE);

        options.setConfig(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerConfig.getBootstrapServers());
        options.setConfig(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerConfig.getKeyDeserializer());
        options.setConfig(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerConfig.getValueDeserializer());
        options.setConfig(ConsumerConfig.GROUP_ID_CONFIG, consumerConfig.getGroupId());
        options.setConfig(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getAutoOffsetReset());
        options.setConfig(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfig.getEnableAutoCommit());

        if (consumerConfig.getKeystoreLocation() != null && consumerConfig.getTruststoreLocation() != null) {
            options.setConfig(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            options.setConfig(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, consumerConfig.getKeystoreLocation());
            if (consumerConfig.getKeystorePassword() != null) {
                options.setConfig(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, consumerConfig.getKeystorePassword());
            }

            options.setConfig(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, consumerConfig.getTruststoreLocation());
            if (consumerConfig.getTruststorePassword() != null) {
                options.setConfig(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, consumerConfig.getTruststorePassword());
            }
        }

        return KafkaClientFactory.createConsumer(vertx, options);
    }

    private static <K, V> io.vertx.kafka.client.producer.KafkaProducer<K, V> createProducer(io.vertx.core.Vertx vertx, KafkaClientOptions options) {
        KafkaWriteStream<K, V> stream = KafkaWriteStream.create(vertx, options);
        return (new KafkaProducerImpl(vertx, stream)).registerCloseHook();
    }

    private static <K, V> io.vertx.rxjava.kafka.client.producer.KafkaProducer<K,V> createProducer(io.vertx.rxjava.core.Vertx vertx, KafkaClientOptions options) {
        return KafkaProducer.newInstance(KafkaClientFactory.createProducer(vertx.getDelegate(), options));
    }

    private static <K, V> io.vertx.kafka.client.consumer.KafkaConsumer<K, V> createConsumer(io.vertx.core.Vertx vertx, KafkaClientOptions options) {
        KafkaReadStream<K, V> stream = KafkaReadStream.create(vertx, options);
        return (new KafkaConsumerImpl(stream)).registerCloseHook();
    }

    private static <K, V> io.vertx.rxjava.kafka.client.consumer.KafkaConsumer<K,V> createConsumer(io.vertx.rxjava.core.Vertx vertx, KafkaClientOptions options) {
        return KafkaConsumer.newInstance(KafkaClientFactory.createConsumer(vertx.getDelegate(), options));
    }
}
