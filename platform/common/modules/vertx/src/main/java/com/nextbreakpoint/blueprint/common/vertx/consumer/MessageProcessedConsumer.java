package com.nextbreakpoint.blueprint.common.vertx.consumer;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageProcessedConsumer<T> implements BiConsumer<Message, T> {
    private final Logger logger = LoggerFactory.getLogger(MessageProcessedConsumer.class.getName());

    @Override
    public void accept(Message message, T output) {
        logger.info("Message processed: id=" + message.getMessageId());
    }
}
