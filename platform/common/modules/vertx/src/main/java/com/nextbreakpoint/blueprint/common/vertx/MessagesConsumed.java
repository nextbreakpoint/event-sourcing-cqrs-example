package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

public class MessagesConsumed implements BiConsumer<List<InputMessage>, Void> {
    private final Logger logger = LoggerFactory.getLogger(MessagesConsumed.class.getName());

    @Override
    public void accept(List<InputMessage> messages, Void ignore) {
        logger.debug("Consumed " + messages.size() + " " + (messages.size() > 1 ? "messages" : "message"));
    }
}
