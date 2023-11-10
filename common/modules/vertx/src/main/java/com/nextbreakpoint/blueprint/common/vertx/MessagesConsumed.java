package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.BiConsumer;

@Log4j2
public class MessagesConsumed<T> implements BiConsumer<List<InputMessage<T>>, Void> {
    @Override
    public void accept(List<InputMessage<T>> messages, Void ignore) {
        log.trace("Consumed {} messages", messages.size());
    }
}
