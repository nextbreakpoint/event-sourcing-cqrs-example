package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.schedulers.Schedulers;

public class KafkaTestEmitter {
    private KafkaProducer<String, String> kafkaProducer;
    private String topic;

    public KafkaTestEmitter(KafkaProducer<String, String> kafkaProducer, String topic) {
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    public void sendMessage(OutputMessage message) {
        kafkaProducer.rxSend(createKafkaRecord(message))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return KafkaProducerRecord.create(topic, message.getKey(), Json.encode(message.getValue()));
    }
}