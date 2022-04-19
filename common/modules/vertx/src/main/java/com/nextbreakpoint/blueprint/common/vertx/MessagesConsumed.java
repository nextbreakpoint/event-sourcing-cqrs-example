package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.BiConsumer;

@Log4j2
public class MessagesConsumed implements BiConsumer<List<InputMessage>, Void> {
    @Override
    public void accept(List<InputMessage> messages, Void ignore) {
        log.debug("Consumed " + messages.size() + " " + (messages.size() > 1 ? "messages" : "message"));
    }
}
