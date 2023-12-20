package com.nextbreakpoint.blueprint.accounts.operations.delete;

import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class DeleteAccountResponseMapperTest {
    private final DeleteAccountResponseMapper mapper = new DeleteAccountResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var uuid = UUID.randomUUID();

        final var response = DeleteAccountResponse.builder()
                .withUuid(uuid)
                .withResult(1)
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("uuid")).isEqualTo(uuid.toString());
        softly.assertAll();
    }
}