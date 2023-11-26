package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.function.BiConsumer;

@Log4j2
public class MessageFailed<T> implements BiConsumer<InputMessage<T>, Throwable> {
    @Override
    public void accept(InputMessage<T> message, Throwable error) {
        log.error("An error occurred while consuming 1 message", error);
    }
}
