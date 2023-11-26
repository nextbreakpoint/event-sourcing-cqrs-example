package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public interface TestActions {
    void clearMessages(Source source);

    void emitMessage(Source source, OutputMessage<Object> message, Function<String, String> router);

    List<InputMessage<Object>> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<Object>> messagePredicate);

    List<Row> fetchMessages(UUID designId, UUID messageId);

    List<Row> fetchDesigns(UUID designId);

    enum Source {
        EVENTS, RENDER
    }
}
