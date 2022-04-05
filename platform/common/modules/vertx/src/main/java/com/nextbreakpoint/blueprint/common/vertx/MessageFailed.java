package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageFailed implements BiConsumer<InputMessage, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(MessageFailed.class.getName());

    @Override
    public void accept(InputMessage message, Throwable error) {
        logger.error("An error occurred while consuming 1 message", error);
    }
}
