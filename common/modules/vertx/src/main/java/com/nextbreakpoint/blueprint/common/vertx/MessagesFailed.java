package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.BiConsumer;

@Log4j2
public class MessagesFailed<T> implements BiConsumer<List<InputMessage<T>>, Throwable> {
    @Override
    public void accept(List<InputMessage<T>> messages, Throwable error) {
        log.error("An error occurred while consuming {} message", messages.size(), error);
    }
}
