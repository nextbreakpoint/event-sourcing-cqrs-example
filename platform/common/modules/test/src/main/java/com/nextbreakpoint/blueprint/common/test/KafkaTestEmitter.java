package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.schedulers.Schedulers;

public class KafkaTestEmitter {
    private KafkaProducer<String, String> kafkaProducer;
    private String topicName;

    public KafkaTestEmitter(KafkaProducer<String, String> kafkaProducer, String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.topicName = topicName;
    }

    public void send(OutputMessage message) {
        kafkaProducer.rxSend(createKafkaRecord(message))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void sendAsync(OutputMessage message) {
        kafkaProducer.rxSend(createKafkaRecord(message))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return KafkaProducerRecord.create(topicName, message.getKey(), Json.encodeValue(message.getValue()));
    }
}