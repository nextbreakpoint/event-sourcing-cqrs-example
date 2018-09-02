package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.Store;
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
    private Logger logger = LoggerFactory.getLogger(InsertDesignController.class);

    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DesignChangedEvent, Message> mapper;

    public InsertDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChangedEvent, Message> mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<CommandResult> onNext(InsertDesignCommand command) {
        return store.insertDesign(command)
                .map(result -> new DesignChangedEvent(result.getUuid(), System.currentTimeMillis()))
                .flatMap(this::sendMessage)
                .map(metadata -> new CommandResult(command.getUuid(), CommandStatus.SUCCESS))
                .onErrorReturn(err -> new CommandResult(command.getUuid(), CommandStatus.FAILURE));
    }

    protected Single<DesignChangedEvent> sendMessage(DesignChangedEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record).map(metadata -> event))
                .doOnError(err -> logger.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> event);
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DesignChangedEvent event) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, event.getUuid().toString(), Json.encode(mapper.transform(event))));
    }
}
