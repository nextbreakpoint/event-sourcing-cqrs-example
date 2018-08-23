package com.nextbreakpoint.shop.common.handlers;

import com.nextbreakpoint.shop.common.model.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class FailedMessageConsumer implements BiConsumer<Message, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(FailedMessageConsumer.class.getName());

    @Override
    public void accept(Message message, Throwable throwable) {
        logger.error("Failed to send message: id=" + message.getMessageId(), throwable);
    }
}
