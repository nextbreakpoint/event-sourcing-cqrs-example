package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.accounts.model.Account;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class LoadAccountResponseMapperTest {
    private final LoadAccountResponseMapper mapper = new LoadAccountResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var uuid = UUID.randomUUID();

        final var account = Account.builder()
                .withUuid(uuid.toString())
                .withName("test")
                .withAuthorities("admin")
                .build();

        final var response = LoadAccountResponse.builder()
                .withUuid(uuid)
                .withAccount(account)
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isPresent();
        final var json = new JsonObject(result.get());
        softly.assertThat(json.getString("uuid")).isEqualTo(uuid.toString());
        softly.assertThat(json.getString("name")).isEqualTo("test");
        softly.assertThat(json.getString("role")).isEqualTo("admin");
        softly.assertAll();
    }
}