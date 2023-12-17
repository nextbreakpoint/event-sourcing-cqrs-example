package com.nextbreakpoint.blueprint.designs.operations.download;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RequestBody;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final RequestBody requestBody = mock();

    private final DownloadDesignRequestMapper mapper = new DownloadDesignRequestMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateRequest(String manifest, String metadata, String script) {
        givenARequest(manifest, metadata, script);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getManifest()).isEqualTo(manifest);
        softly.assertThat(request.getMetadata()).isEqualTo(metadata);
        softly.assertThat(request.getScript()).isEqualTo(script);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenBodyIsEmpty() {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body is empty");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainScript() {
        givenARequest(MANIFEST, METADATA, null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: script is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainMetadata() {
        givenARequest(MANIFEST, null, SCRIPT);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: metadata is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainManifest() {
        givenARequest(null, METADATA, SCRIPT);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: manifest is missing");
    }

    private void givenARequest(String manifest, String metadata, String script) {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", manifest, "script", script, "metadata", metadata));
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(MANIFEST, METADATA, SCRIPT),
                Arguments.of(INVALID_MANIFEST, METADATA, SCRIPT)
        );
    }
}