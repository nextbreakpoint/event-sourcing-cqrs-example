package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.CommandStatus;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(UpdateDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<UpdateDesignCommand, OutputMessage> mapper;

    public UpdateDesignController(String topic, KafkaProducer<String, String> producer, Mapper<UpdateDesignCommand, OutputMessage> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return createRecord(request)
                .flatMap(producer::rxWrite)
                .doOnError(err -> logger.error("Can't publish message", err))
                .map(record -> new UpdateDesignResponse(request.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new UpdateDesignResponse(request.getUuid(), CommandStatus.FAILURE));
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(UpdateDesignRequest request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(request), createValue(request)));
    }

    private String createKey(UpdateDesignRequest request) {
        return request.getUuid().toString();
    }

    private String createValue(UpdateDesignRequest request) {
        return Json.encode(mapper.transform(new UpdateDesignCommand(request.getUuid(), request.getJson(), System.currentTimeMillis())));
    }
}
