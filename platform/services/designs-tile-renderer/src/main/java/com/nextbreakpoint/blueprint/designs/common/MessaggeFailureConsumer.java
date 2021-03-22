package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessaggeFailureConsumer implements BiConsumer<RecordAndMessage, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeFailureConsumer.class.getName());

    @Override
    public void accept(RecordAndMessage message, Throwable error) {
        logger.info("Can't process message " + message.getMessage().getMessageId(), error);
    }
}
