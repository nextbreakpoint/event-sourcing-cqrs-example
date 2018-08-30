package com.nextbreakpoint.shop.common.vertx.handlers;

import com.nextbreakpoint.shop.common.model.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageProcessedConsumer<T> implements BiConsumer<Message, T> {
    private final Logger logger = LoggerFactory.getLogger(MessageProcessedConsumer.class.getName());

    @Override
    public void accept(Message message, T output) {
        logger.info("Message processed: id=" + message.getMessageId());
    }
}
