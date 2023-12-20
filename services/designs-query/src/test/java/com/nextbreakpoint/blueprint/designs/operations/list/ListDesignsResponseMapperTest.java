package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;

class ListDesignsResponseMapperTest {
    private final ListDesignsResponseMapper mapper = new ListDesignsResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var instant = Instant.now(Clock.systemUTC());

        final var design1 = TestUtils.aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, instant, instant, LEVELS_READY, 100, true, DATA_1);
        final var design2 = TestUtils.aDesign(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, instant, instant, LEVELS_READY, 50, false, DATA_2);

        final var response = ListDesignsResponse.builder()
                .withTotal(2)
                .withDesigns(List.of(design1, design2))
                .build();

        final var json = new JsonObject(mapper.transform(response));

        final var expectedDesign1 = new JsonObject(Json.encode(DesignDocument.from(design1)));
        final var expectedDesign2 = new JsonObject(Json.encode(DesignDocument.from(design2)));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getInteger("total")).isEqualTo(2);
        softly.assertThat(json.getJsonArray("designs")).hasSize(2);
        softly.assertThat(json.getJsonArray("designs")).containsExactlyInAnyOrder(expectedDesign1, expectedDesign2);
        softly.assertAll();
    }

    @Test
    void shouldReturnAnEmptyListWhenDesignsAreNotFound() {
        final var response = ListDesignsResponse.builder()
                .withTotal(0)
                .withDesigns(List.of())
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getInteger("total")).isEqualTo(0);
        softly.assertThat(json.getJsonArray("designs")).hasSize(0);
        softly.assertAll();
    }
}