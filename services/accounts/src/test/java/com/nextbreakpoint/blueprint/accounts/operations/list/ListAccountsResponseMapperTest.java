package com.nextbreakpoint.blueprint.accounts.operations.list;

import io.vertx.core.json.JsonArray;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class ListAccountsResponseMapperTest {
    private final ListAccountsResponseMapper mapper = new ListAccountsResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var uuid1 = UUID.randomUUID();
        final var uuid2 = UUID.randomUUID();

        final var response = ListAccountsResponse.builder()
                .withUuids(List.of(uuid1.toString(), uuid2.toString()))
                .build();

        final var json = new JsonArray(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json).hasSize(2);
        softly.assertThat(json.getString(0)).isEqualTo(uuid1.toString());
        softly.assertThat(json.getString(1)).isEqualTo(uuid2.toString());
        softly.assertAll();
    }

    @Test
    void shouldReturnEmptyArrayWhenAccountsAreNotPresent() {
        final var response = ListAccountsResponse.builder()
                .withUuids(List.of())
                .build();

        final var json = new JsonArray(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json).isEmpty();
        softly.assertAll();
    }
}