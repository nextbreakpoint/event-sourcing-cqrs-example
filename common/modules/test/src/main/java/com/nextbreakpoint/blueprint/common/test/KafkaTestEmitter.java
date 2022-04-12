package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

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
        Single.fromCallable(() -> kafkaProducer.send(createKafkaRecord(message, topicName)))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void sendAsync(OutputMessage message, String topicName) {
        Single.fromCallable(() -> kafkaProducer.send(createKafkaRecord(message, topicName)))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private ProducerRecord<String, String> createKafkaRecord(OutputMessage message, String topicName) {
        return new ProducerRecord<>(topicName, null, message.getKey(), Json.encodeValue(message.getValue()), makeHeaders(message));
    }

    private List<Header> makeHeaders(OutputMessage message) {
        return new ArrayList<>();
    }

    public String getTopicName() {
        return topicName;
    }
}