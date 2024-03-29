package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.function.BiConsumer;

@Log4j2
public class MessageConsumed<T> implements BiConsumer<InputMessage<T>, Void> {
    @Override
    public void accept(InputMessage<T> message, Void ignore) {
        log.trace("Consumed 1 message");
    }
}
