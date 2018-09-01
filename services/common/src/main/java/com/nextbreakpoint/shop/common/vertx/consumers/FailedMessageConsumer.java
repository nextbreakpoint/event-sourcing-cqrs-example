package com.nextbreakpoint.shop.common.vertx.consumers;

import com.nextbreakpoint.shop.common.model.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class FailedMessageConsumer implements BiConsumer<Message, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(FailedMessageConsumer.class.getName());

    @Override
    public void accept(Message message, Throwable error) {
        logger.error("Failed to process message: id=" + message.getMessageId(), error);
    }
}
