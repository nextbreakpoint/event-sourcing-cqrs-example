package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessaggeFailureConsumer implements BiConsumer<Message, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeFailureConsumer.class.getName());

    @Override
    public void accept(Message message, Throwable error) {
        logger.error("An error occurred while consuming message " + message.getMessageId(), error);
    }
}
