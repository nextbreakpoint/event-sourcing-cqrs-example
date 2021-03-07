package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.designs.model.CommandResult;
import com.nextbreakpoint.blueprint.designs.model.CommandStatus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesign, CommandResult> {
    private Logger LOG = LoggerFactory.getLogger(UpdateDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<UpdateDesign, Message> mapper;

    public UpdateDesignController(String topic, KafkaProducer<String, String> producer, Mapper<UpdateDesign, Message> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<CommandResult> onNext(UpdateDesign command) {
        return createRecord(command)
                .flatMap(record -> producer.rxWrite(record))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .map(record -> new CommandResult(command.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new CommandResult(command.getUuid(), CommandStatus.FAILURE));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(UpdateDesign command) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, command.getUuid().toString(), Json.encode(mapper.transform(command))));
    }
}
