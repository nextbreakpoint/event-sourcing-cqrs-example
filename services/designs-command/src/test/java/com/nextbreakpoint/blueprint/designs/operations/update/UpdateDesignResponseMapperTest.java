package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateDesignResponseMapperTest {
    private final UpdateDesignResponseMapper mapper = new UpdateDesignResponseMapper();

    @Test
    void shouldCreateResponse() {
        final UUID uuid = UUID.randomUUID();

        final var response = UpdateDesignResponse.builder()
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

        final var response = UpdateDesignResponse.builder()
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