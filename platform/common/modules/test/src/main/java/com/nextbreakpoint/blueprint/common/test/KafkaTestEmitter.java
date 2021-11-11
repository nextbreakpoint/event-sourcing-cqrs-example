package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.vertx.core.json.Json;
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

    public void sendMessage(OutputMessage message) {
        kafkaProducer.rxSend(createKafkaRecord(message))
                .doOnEach(action -> System.out.println("Sending message to topic " + topicName + ": " + message))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return KafkaProducerRecord.create(topicName, message.getKey(), Json.encode(message.getValue()));
    }
}