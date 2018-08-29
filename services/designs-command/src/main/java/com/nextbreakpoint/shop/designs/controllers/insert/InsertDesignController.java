package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import com.nextbreakpoint.shop.designs.model.CommandStatus;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignCommand, CommandResult> {
    private Logger LOG = LoggerFactory.getLogger(InsertDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<InsertDesignCommand, Message> mapper;

    public InsertDesignController(String topic, KafkaProducer<String, String> producer, Mapper<InsertDesignCommand, Message> mapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<CommandResult> onNext(InsertDesignCommand command) {
        return createRecord(command)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new CommandResult(command.getUuid(), CommandStatus.SUCCESS))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new CommandResult(command.getUuid(), CommandStatus.FAILURE));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(InsertDesignCommand command) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, command.getUuid().toString(), Json.encode(mapper.transform(command))));
    }
}
