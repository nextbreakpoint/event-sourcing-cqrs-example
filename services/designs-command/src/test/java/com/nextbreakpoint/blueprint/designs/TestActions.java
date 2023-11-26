package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public interface TestActions {
    void clearMessages(Source source);

    List<InputMessage<Object>> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<Object>> messagePredicate);

    List<Row> fetchMessages(UUID designId, UUID messageId);

    String makeAuthorization(UUID userId, String authority);

    UUID submitInsertDesignRequest(String authorization, Map<String, String> design) throws MalformedURLException;

    void submitUpdateDesignRequest(String authorization, Map<String, String> design, UUID designId) throws MalformedURLException;

    void submitDeleteDesignRequest(String authorization, UUID designId) throws MalformedURLException;

    enum Source {
        EVENTS, COMMANDS
    }
}
