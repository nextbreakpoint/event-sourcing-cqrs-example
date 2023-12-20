package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class GetTileResponseMapperTest {
    private final GetTileResponseMapper mapper = new GetTileResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var response = GetTileResponse.builder()
                .withImage(new Image(new byte[] { 0, 1, 2 }, "012"))
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isPresent();
        softly.assertThat(result).hasValue(new Image(new byte[] { 0, 1, 2 }, "012"));
        softly.assertAll();
    }

    @Test
    void shouldReturnNothingWhenImageIsNull() {
        final var response = GetTileResponse.builder()
                .withImage(null)
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isNotPresent();
        softly.assertAll();
    }
}