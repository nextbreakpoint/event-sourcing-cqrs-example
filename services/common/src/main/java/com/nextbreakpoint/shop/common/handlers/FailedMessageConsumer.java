package com.nextbreakpoint.shop.common.handlers;

import com.nextbreakpoint.shop.common.model.Message;

import java.util.function.BiConsumer;

public class FailedMessageConsumer implements BiConsumer<Message, Throwable> {
    @Override
    public void accept(Message message, Throwable throwable) {
    }
}
