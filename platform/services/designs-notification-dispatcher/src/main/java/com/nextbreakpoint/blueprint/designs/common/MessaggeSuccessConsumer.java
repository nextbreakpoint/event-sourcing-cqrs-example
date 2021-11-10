package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public class MessaggeSuccessConsumer implements BiConsumer<InputMessage, JsonObject> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeSuccessConsumer.class.getName());

    @Override
    public void accept(InputMessage message, JsonObject object) {
        logger.info("Message consumed " + message.getValue().getUuid());
    }
}
