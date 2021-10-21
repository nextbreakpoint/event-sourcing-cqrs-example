package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.vertx.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public class EventSuccessConsumer implements BiConsumer<RecordAndMessage, JsonObject> {
    private final Logger logger = LoggerFactory.getLogger(EventSuccessConsumer.class.getName());

    @Override
    public void accept(RecordAndMessage message, JsonObject object) {
        logger.info("Message consumed " + message.getMessage().getMessageId());
    }
}
