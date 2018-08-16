package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignEvent, UpdateDesignResponse> {
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<UpdateDesignEvent, Message> messageMapper;

    public UpdateDesignController(String topic, KafkaProducer<String, String> producer, Mapper<UpdateDesignEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new UpdateDesignResponse(event.getUuid(), 1))
                .onErrorReturn(record -> new UpdateDesignResponse(event.getUuid(), 0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(UpdateDesignEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, request.getUuid().toString(), Json.encode(messageMapper.transform(request))));
    }
}
