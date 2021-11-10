package com.nextbreakpoint.blueprint.designs.operations.insert;

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

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(InsertDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<InsertDesignCommand, OutputMessage> mapper;

    public InsertDesignController(String topic, KafkaProducer<String, String> producer, Mapper<InsertDesignCommand, OutputMessage> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return createRecord(request)
                .flatMap(producer::rxWrite)
                .doOnError(err -> logger.error("Can't publish message", err))
                .map(record -> new InsertDesignResponse(request.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new InsertDesignResponse(request.getUuid(), CommandStatus.FAILURE));
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(InsertDesignRequest request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(request), createValue(request)));
    }

    private String createKey(InsertDesignRequest request) {
        return request.getUuid().toString();
    }

    private String createValue(InsertDesignRequest request) {
        return Json.encode(mapper.transform(new InsertDesignCommand(request.getUuid(), request.getJson(), System.currentTimeMillis())));
    }
}
