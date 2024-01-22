package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class GetImageResponseMapperTest {
    private final GetImageResponseMapper mapper = new GetImageResponseMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateResponse(String checksum, byte[] bytes) {
        final var response = GetImageResponse.builder()
                .withImage(new Image(bytes, checksum))
                .build();

        final var image = mapper.transform(response);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(image).hasValue(new Image(bytes, checksum));
        softly.assertAll();
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of("123", new byte[] { 1, 2, 3 }),
                Arguments.of("456", new byte[] { 1, 2, 3, 4, 5, 6 })
        );
    }
}