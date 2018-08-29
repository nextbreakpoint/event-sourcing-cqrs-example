package com.nextbreakpoint.shop.common.handlers;

import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
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
