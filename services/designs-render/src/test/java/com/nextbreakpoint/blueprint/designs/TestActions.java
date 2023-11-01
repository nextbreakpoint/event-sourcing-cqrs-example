package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface TestActions {
    void clearMessages(Source source);

    void emitMessage(Source source, OutputMessage message, Function<String, String> router);

    List<InputMessage> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate);

    byte[] getImage(String bucketKey);

    enum Source {
        RENDER;
    }
}
