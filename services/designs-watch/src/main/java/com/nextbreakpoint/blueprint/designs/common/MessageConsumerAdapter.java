package com.nextbreakpoint.blueprint.designs.common;

import io.vertx.rxjava.core.eventbus.MessageConsumer;

import java.util.Objects;

public class MessageConsumerAdapter {
    private final MessageConsumer<String> consumer;

    public MessageConsumerAdapter(MessageConsumer<String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    public void unregister() {
        consumer.unregister();
    }
}
