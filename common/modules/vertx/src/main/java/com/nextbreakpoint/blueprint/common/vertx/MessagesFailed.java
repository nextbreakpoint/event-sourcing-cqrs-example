package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.BiConsumer;

@Log4j2
public class MessagesFailed implements BiConsumer<List<InputMessage>, Throwable> {
    @Override
    public void accept(List<InputMessage> messages, Throwable error) {
        log.error("An error occurred while consuming {} message{}", messages.size(), messages.size() > 1 ? "s" : "", error);
    }
}
