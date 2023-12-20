
package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Header;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class KafkaTestEmitter<T, R> {
    private final KafkaProducer<String, R> kafkaProducer;
    private final Mapper<OutputRecord<T>, ProducerRecord<String, R>> recordMapper;
    private final String topicName;

    public KafkaTestEmitter(KafkaProducer<String, R> kafkaProducer, Mapper<OutputRecord<T>, ProducerRecord<String, R>> recordMapper, String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.recordMapper = recordMapper;
        this.topicName = topicName;
    }

    public void send(OutputMessage<T> message) {
        send(message, topicName);
    }

    public void sendAsync(OutputMessage<T> message) {
        sendAsync(message, topicName);
    }

    public void send(OutputMessage<T> message, String topicName) {
        Single.fromCallable(() -> kafkaProducer.send(createKafkaRecord(message, topicName)))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void sendAsync(OutputMessage<T> message, String topicName) {
        Single.fromCallable(() -> kafkaProducer.send(createKafkaRecord(message, topicName)))
                .doOnEach(action -> System.out.println("Sending message " + message + " to topic " + topicName))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private ProducerRecord<String, R> createKafkaRecord(OutputMessage<T> message, String topicName) {
        final OutputRecord<T> record = OutputRecord.<T>builder()
                .withKey(message.getKey())
                .withTopicName(topicName)
                .withPayload(message.getValue())
                .withHeaders(makeHeaders(message))
                .build();

        return recordMapper.transform(record);
    }

    private List<Header> makeHeaders(OutputMessage<T> message) {
        return new ArrayList<>();
    }

    public String getTopicName() {
        return topicName;
    }
}