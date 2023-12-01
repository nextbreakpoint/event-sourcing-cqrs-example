package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InsertDesignResponseMapperTest {
    private final InsertDesignResponseMapper mapper = new InsertDesignResponseMapper();

    @Test
    void shouldCreateResponse() {
        final UUID uuid = UUID.randomUUID();

        final var response = InsertDesignResponse.builder()
                .withUuid(uuid)
                .withStatus(ResultStatus.SUCCESS)
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("uuid")).isEqualTo(uuid.toString());
        softly.assertThat(json.getString("status")).isEqualTo("SUCCESS");
        softly.assertAll();
    }

    @Test
    void shouldCreateUnsuccessfulResponse() {
        final UUID uuid = UUID.randomUUID();

        final var response = InsertDesignResponse.builder()
                .withUuid(uuid)
                .withStatus(ResultStatus.FAILURE)
                .withError("some error")
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("uuid")).isEqualTo(uuid.toString());
        softly.assertThat(json.getString("status")).isEqualTo("FAILURE");
        softly.assertThat(json.getString("error")).isEqualTo("some error");
        softly.assertAll();
    }
}