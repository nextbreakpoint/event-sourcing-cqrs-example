package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageFailureConsumer implements BiConsumer<Message, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessageFailureConsumer.class.getName());

    @Override
    public void accept(Message message, Throwable error) {
        logger.error("An error occurred while consuming message " + message.getPayload().getUuid(), error);
    }
}
