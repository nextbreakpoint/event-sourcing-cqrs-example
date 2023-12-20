package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;

class LoadDesignResponseMapperTest {
    private final LoadDesignResponseMapper mapper = new LoadDesignResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var instant = Instant.now(Clock.systemUTC());

        final var design = TestUtils.aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, instant, instant, LEVELS_READY, 100, true, DATA_1);

        final var response = LoadDesignResponse.builder()
                .withDesign(design)
                .build();

        final var result = mapper.transform(response);

        final var expectedDesign = new JsonObject(Json.encode(DesignDocument.from(design)));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isPresent();
        final var json = new JsonObject(result.get());
        softly.assertThat(json).isEqualTo(expectedDesign);
        softly.assertAll();
    }

    @Test
    void shouldReturnNothingWhenDesignIsNotFound() {
        final var response = LoadDesignResponse.builder()
                .withDesign(null)
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isNotPresent();
        softly.assertAll();
    }
}