package com.nextbreakpoint.blueprint.designs.operations.delete;

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

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(DeleteDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DeleteDesignCommand, OutputMessage> mapper;

    public DeleteDesignController(String topic, KafkaProducer<String, String> producer, Mapper<DeleteDesignCommand, OutputMessage> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return createRecord(request)
                .flatMap(producer::rxWrite)
                .doOnError(err -> logger.error("Can't publish message", err))
                .map(record -> new DeleteDesignResponse(request.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new DeleteDesignResponse(request.getUuid(), CommandStatus.FAILURE));
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignRequest request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(request), createValue(request)));
    }

    private String createKey(DeleteDesignRequest request) {
        return request.getUuid().toString();
    }

    private String createValue(DeleteDesignRequest request) {
        return Json.encode(mapper.transform(new DeleteDesignCommand(request.getUuid(), System.currentTimeMillis())));
    }
}
