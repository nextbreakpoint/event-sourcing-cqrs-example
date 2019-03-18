package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import com.nextbreakpoint.shop.designs.model.CommandStatus;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignCommand, CommandResult> {
    private Logger LOG = LoggerFactory.getLogger(UpdateDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<UpdateDesignCommand, Message> mapper;

    public UpdateDesignController(String topic, KafkaProducer<String, String> producer, Mapper<UpdateDesignCommand, Message> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<CommandResult> onNext(UpdateDesignCommand command) {
        return createRecord(command)
                .flatMap(record -> producer.rxWrite(record))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .map(record -> new CommandResult(command.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new CommandResult(command.getUuid(), CommandStatus.FAILURE));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(UpdateDesignCommand command) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, command.getUuid().toString(), Json.encode(mapper.transform(command))));
    }
}
