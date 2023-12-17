package com.nextbreakpoint.blueprint.designs.operations.upload;

import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;

class UploadDesignResponseMapperTest {
    private final UploadDesignResponseMapper mapper = new UploadDesignResponseMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateResponse(String manifest, String metadata, String script, List<String> errors) {
        final var response = UploadDesignResponse.builder()
                .withManifest(manifest)
                .withMetadata(metadata)
                .withScript(script)
                .withErrors(errors)
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("manifest")).isEqualTo(manifest);
        softly.assertThat(json.getString("metadata")).isEqualTo(metadata);
        softly.assertThat(json.getString("script")).isEqualTo(script);
        softly.assertThat(json.getJsonArray("errors")).hasSize(errors.size());
        IntStream.range(0, errors.size())
                .forEach(index -> softly.assertThat(json.getJsonArray("errors").getString(index)).isEqualTo(errors.get(index)));
        softly.assertAll();
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(MANIFEST, METADATA, SCRIPT, List.of()),
                Arguments.of(MANIFEST, METADATA, SCRIPT, List.of("some error")),
                Arguments.of(MANIFEST, METADATA, SCRIPT, List.of("some error", "some other error")),
                Arguments.of(INVALID_MANIFEST, METADATA, SCRIPT, List.of())
        );
    }
}