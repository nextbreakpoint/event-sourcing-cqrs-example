package com.nextbreakpoint.blueprint.designs.common;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;

import java.util.function.BiConsumer;

public class MessaggeFailureConsumer implements BiConsumer<RecordAndMessage, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeFailureConsumer.class.getName());

    @Override
    public void accept(RecordAndMessage message, Throwable error) {
        logger.error("An error occurred while consuming message " + message.getMessage().getUuid(), error);
    }
}
