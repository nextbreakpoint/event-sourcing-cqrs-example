package com.nextbreakpoint.shop.common.handlers;

import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageReceipt;

import java.util.function.BiConsumer;

public class MessageReceiptConsumer implements BiConsumer<Message, MessageReceipt> {
    @Override
    public void accept(Message message, MessageReceipt messageReceipt) {
    }
}
