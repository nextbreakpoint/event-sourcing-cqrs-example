package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import com.nextbreakpoint.shop.designs.model.CommandStatus;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignCommand, CommandResult> {
    private Logger LOG = LoggerFactory.getLogger(DeleteDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DeleteDesignCommand, Message> mapper;

    public DeleteDesignController(String topic, KafkaProducer<String, String> producer, Mapper<DeleteDesignCommand, Message> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<CommandResult> onNext(DeleteDesignCommand command) {
        return createRecord(command)
                .flatMap(record -> producer.rxWrite(record))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .map(record -> new CommandResult(command.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new CommandResult(command.getUuid(), CommandStatus.FAILURE));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignCommand command) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, command.getUuid().toString(), Json.encode(mapper.transform(command))));
    }
}
