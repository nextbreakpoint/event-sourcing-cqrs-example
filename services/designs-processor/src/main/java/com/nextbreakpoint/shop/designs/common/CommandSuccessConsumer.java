package com.nextbreakpoint.shop.designs.common;

import com.nextbreakpoint.shop.designs.model.CommandRequest;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.function.BiConsumer;

public class CommandSuccessConsumer implements BiConsumer<CommandRequest, CommandResult> {
    private final Logger logger = LoggerFactory.getLogger(CommandSuccessConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public CommandSuccessConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(CommandRequest request, CommandResult result) {
        consumer.commit();
        logger.info("Design changed: id=" + result.getEvent().getUuid());
    }
}
