package com.nextbreakpoint.blueprint.accounts.operations.insert;

import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class InsertAccountResponseMapperTest {
    private final InsertAccountResponseMapper mapper = new InsertAccountResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var uuid = UUID.randomUUID();

        final var response = InsertAccountResponse.builder()
                .withUuid(uuid)
                .withResult(1)
                .withAuthorities("admin")
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("uuid")).isEqualTo(uuid.toString());
        softly.assertThat(json.getString("role")).isEqualTo("admin");
        softly.assertAll();
    }
}