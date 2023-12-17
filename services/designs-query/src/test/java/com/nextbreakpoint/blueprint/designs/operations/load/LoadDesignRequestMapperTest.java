package com.nextbreakpoint.blueprint.designs.operations.load;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();

    private final LoadDesignRequestMapper mapper = new LoadDesignRequestMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateRequest(UUID designId, boolean draft) {
        givenARequest(designId.toString(), String.valueOf(draft));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.isDraft()).isEqualTo(draft);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMissing() {
        givenARequest(null, "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("designId is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMalformed() {
        givenARequest("abc", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMissing() {
        final var designId = UUID.randomUUID();
        givenARequest(designId.toString(), null);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMalformed() {
        final var designId = UUID.randomUUID();
        givenARequest(designId.toString(), "a");

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    private void givenARequest(String designId, String draft) {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("designId")).thenReturn(designId);
        when(httpRequest.getParam("draft", "false")).thenReturn(draft != null ? draft : "false");
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(new UUID(0L, 1L), false),
                Arguments.of(new UUID(0L, 2L), false),
                Arguments.of(new UUID(0L, 2L), true)
        );
    }
}