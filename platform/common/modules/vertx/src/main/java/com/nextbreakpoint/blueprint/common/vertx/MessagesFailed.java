package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

public class MessagesFailed implements BiConsumer<List<InputMessage>, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessagesFailed.class.getName());

    @Override
    public void accept(List<InputMessage> messages, Throwable error) {
        logger.error("An error occurred while consuming " + messages.size() + " " + (messages.size() > 1 ? "messages" : "message"), error);
    }
}
