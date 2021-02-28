package com.nextbreakpoint.blueprint.common.vertx.consumer;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessaggeFailureConsumer implements BiConsumer<Message, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeFailureConsumer.class.getName());

    @Override
    public void accept(Message message, Throwable error) {
        logger.info("Message failed: id=" + message.getMessageId(), error);
    }
}
