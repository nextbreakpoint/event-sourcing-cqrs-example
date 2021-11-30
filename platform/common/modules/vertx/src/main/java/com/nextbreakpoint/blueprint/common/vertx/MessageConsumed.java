package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageConsumed implements BiConsumer<InputMessage, Void> {
    private final Logger logger = LoggerFactory.getLogger(MessageConsumed.class.getName());

    @Override
    public void accept(InputMessage message, Void ignore) {
        logger.info("Message consumed " + message);
    }
}
