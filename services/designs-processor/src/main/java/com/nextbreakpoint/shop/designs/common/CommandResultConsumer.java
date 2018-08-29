package com.nextbreakpoint.shop.designs.common;

import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class CommandResultConsumer implements BiConsumer<Message, CommandResult> {
    private final Logger logger = LoggerFactory.getLogger(CommandResultConsumer.class.getName());

    @Override
    public void accept(Message message, CommandResult result) {
        logger.info("Message processed: id=" + message.getMessageId() + ", status=" + result.getStatus());
    }
}
