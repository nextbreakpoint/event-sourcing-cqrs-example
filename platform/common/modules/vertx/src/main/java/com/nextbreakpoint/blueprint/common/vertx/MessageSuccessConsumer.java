package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public class MessageSuccessConsumer implements BiConsumer<Message, Void> {
    private final Logger logger = LoggerFactory.getLogger(MessageSuccessConsumer.class.getName());

    @Override
    public void accept(Message message, Void ignore) {
        logger.info("Message consumed " + message.getUuid());
    }
}
