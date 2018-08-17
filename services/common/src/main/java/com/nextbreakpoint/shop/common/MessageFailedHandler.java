package com.nextbreakpoint.shop.common;

import java.util.function.BiConsumer;

public class MessageFailedHandler implements BiConsumer<Message, Throwable> {
    @Override
    public void accept(Message message, Throwable throwable) {
    }
}
