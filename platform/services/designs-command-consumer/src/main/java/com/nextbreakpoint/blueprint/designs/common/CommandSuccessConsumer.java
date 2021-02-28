package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.model.CommandOutput;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.function.BiConsumer;

public class CommandSuccessConsumer implements BiConsumer<RecordAndMessage, CommandOutput> {
    private final Logger logger = LoggerFactory.getLogger(CommandSuccessConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public CommandSuccessConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(RecordAndMessage input, CommandOutput output) {
        consumer.commit();
        logger.info("Design changed: id=" + output.getEvent().getUuid());
    }
}
