package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.vertx.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class EventFailureConsumer implements BiConsumer<RecordAndMessage, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(EventFailureConsumer.class.getName());

    @Override
    public void accept(RecordAndMessage message, Throwable error) {
        logger.error("An error occurred while consuming message " + message.getMessage().getMessageId(), error);
    }
}
