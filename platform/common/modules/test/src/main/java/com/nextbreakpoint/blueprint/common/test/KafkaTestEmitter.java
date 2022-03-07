package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

public class KafkaTestEmitter {
    private KafkaProducer<String, String> kafkaProducer;
    private String topicName;

    public KafkaTestEmitter(KafkaProducer<String, String> kafkaProducer, String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.topicName = topicName;
    }

    public void send(OutputMessage message) {
        send(message, topicName);
    }

    public void sendAsync(OutputMessage message) {
        sendAsync(message, topicName);
    }

    public void send(OutputMessage message, String topicName) {
        kafkaProducer.rxSend(createKafkaRecord(message, topicName))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void sendAsync(OutputMessage message, String topicName) {
        kafkaProducer.rxSend(createKafkaRecord(message, topicName))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message, String topicName) {
        final KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topicName, message.getKey(), Json.encodeValue(message.getValue()));
        record.addHeaders(makeHeaders(message));
        return record;
    }

    private List<KafkaHeader> makeHeaders(OutputMessage message) {
        return message.getTrace().toHeaders().entrySet().stream()
                .map(e -> KafkaHeader.header(e.getKey(), e.getValue()))
//                .peek(header -> System.out.println("header: " + header.key() + "=" + header.value()))
                .collect(Collectors.toList());
    }

    public String getTopicName() {
        return topicName;
    }
}