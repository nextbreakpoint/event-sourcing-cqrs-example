package com.nextbreakpoint.blueprint.designs.operations.download;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class DownloadDesignResponseMapperTest {
    private final DownloadDesignResponseMapper mapper = new DownloadDesignResponseMapper();

    @Test
    void shouldCreateResponse() {
        final var response = DownloadDesignResponse.builder()
                .withBytes(new byte[] { 0, 1, 2 })
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isEqualTo(new byte[] { 0, 1, 2 });
        softly.assertAll();
    }

    @Test
    void shouldReturnEmptyArrayWhenBytesAreNull() {
        final var response = DownloadDesignResponse.builder()
                .withBytes(null)
                .build();

        final var result = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).isEqualTo(new byte[0]);
        softly.assertAll();
    }
}