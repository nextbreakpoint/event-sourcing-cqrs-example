package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public interface TestActions {
    void clearMessages(Source source);

    void emitMessage(Source source, OutputMessage message, Function<String, String> router);

    List<InputMessage> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate);

    List<Design> findDesigns(UUID designId);

    List<Design> findDraftDesigns(UUID designId);

    enum Source {
        EVENTS;
    }
}
