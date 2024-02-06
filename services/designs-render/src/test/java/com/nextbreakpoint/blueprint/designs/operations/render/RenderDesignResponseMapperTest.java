package com.nextbreakpoint.blueprint.designs.operations.render;

import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class RenderDesignResponseMapperTest {
    private final RenderDesignResponseMapper mapper = new RenderDesignResponseMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateResponse(String checksum, List<String> errors) {
        final var response = RenderDesignResponse.builder()
                .withChecksum(checksum)
                .withErrors(errors)
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("checksum")).isEqualTo(checksum);
        softly.assertThat(json.getJsonArray("errors")).hasSize(errors.size());
        IntStream.range(0, errors.size())
                .forEach(index -> softly.assertThat(json.getJsonArray("errors").getString(index)).isEqualTo(errors.get(index)));
        softly.assertAll();
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of("123", List.of()),
                Arguments.of("456", List.of("some error")),
                Arguments.of("456", List.of("some error", "some other error"))
        );
    }
}