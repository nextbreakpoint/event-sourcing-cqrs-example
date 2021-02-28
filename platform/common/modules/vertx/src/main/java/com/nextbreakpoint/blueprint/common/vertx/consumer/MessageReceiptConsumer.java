package com.nextbreakpoint.blueprint.common.vertx.consumer;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageReceipt;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class MessageReceiptConsumer implements BiConsumer<Message, MessageReceipt> {
    private final Logger logger = LoggerFactory.getLogger(MessageReceiptConsumer.class.getName());

    @Override
    public void accept(Message message, MessageReceipt messageReceipt) {
        logger.info("Message sent: id=" + message.getMessageId() + ", timestamp=" + messageReceipt.getTimestamp());
    }
}
