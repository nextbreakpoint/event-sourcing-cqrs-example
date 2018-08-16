package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.DeleteDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignEvent, DeleteDesignResponse> {
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DeleteDesignEvent, Message> messageMapper;

    public DeleteDesignController(String topic, KafkaProducer<String, String> producer, Mapper<DeleteDesignEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new DeleteDesignResponse(event.getUuid(), 1))
                .onErrorReturn(err -> new DeleteDesignResponse(event.getUuid(), 0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, request.getUuid().toString(), Json.encode(messageMapper.transform(request))));
    }
}
