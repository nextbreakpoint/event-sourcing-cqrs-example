package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.function.BiConsumer;

@Log4j2
public class MessageFailed implements BiConsumer<InputMessage, Throwable> {
    @Override
    public void accept(InputMessage message, Throwable error) {
        log.error("An error occurred while consuming 1 message", error);
    }
}
