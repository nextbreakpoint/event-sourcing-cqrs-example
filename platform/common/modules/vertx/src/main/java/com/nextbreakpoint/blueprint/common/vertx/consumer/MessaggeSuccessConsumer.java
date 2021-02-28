package com.nextbreakpoint.blueprint.common.vertx.consumer;

import com.nextbreakpoint.blueprint.common.core.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessaggeSuccessConsumer implements BiConsumer<Message, JsonObject> {
    private final Logger logger = LoggerFactory.getLogger(MessaggeSuccessConsumer.class.getName());

    @Override
    public void accept(Message message, JsonObject object) {
        logger.info("Message succeeded: id=" + message.getMessageId());
    }
}
