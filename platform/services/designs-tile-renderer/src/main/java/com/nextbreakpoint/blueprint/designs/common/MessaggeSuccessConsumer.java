package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public class MessaggeSuccessConsumer implements BiConsumer<RecordAndMessage, JsonObject> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeSuccessConsumer.class.getName());

    @Override
    public void accept(RecordAndMessage message, JsonObject object) {
        logger.info("Message consumed " + message.getMessage().getMessageId());
    }
}
