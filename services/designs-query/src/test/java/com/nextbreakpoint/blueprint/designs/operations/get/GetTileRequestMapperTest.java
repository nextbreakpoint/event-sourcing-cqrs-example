package com.nextbreakpoint.blueprint.designs.operations.get;

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

class GetTileRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();

    private final GetTileRequestMapper mapper = new GetTileRequestMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateRequest(UUID designId, int level, int col, int row, int size, boolean draft) {
        givenARequest(designId.toString(), String.valueOf(level), String.valueOf(col), String.valueOf(row), String.valueOf(size), String.valueOf(draft));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.getLevel()).isEqualTo(level);
        softly.assertThat(request.getCol()).isEqualTo(col);
        softly.assertThat(request.getRow()).isEqualTo(row);
        softly.assertThat(request.getSize()).isEqualTo(size);
        softly.assertThat(request.isDraft()).isEqualTo(draft);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMissing() {
        givenARequest(null, "3", "0", "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("designId is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterLevelIsMissing() {
        givenARequest(UUID.randomUUID().toString(), null, "0", "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("level is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterColIsMissing() {
        givenARequest(UUID.randomUUID().toString(), "3", null, "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("col is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterRowIsMissing() {
        givenARequest(UUID.randomUUID().toString(), "3", "0", null, "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("row is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterSizeIsMissing() {
        givenARequest(UUID.randomUUID().toString(), "3", "0", "1", null, "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("size is missing");
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMissing() {
        final var designId = UUID.randomUUID();
        givenARequest(designId.toString(), "3", "0", "1", "256", null);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.getLevel()).isEqualTo(3);
        softly.assertThat(request.getCol()).isEqualTo(0);
        softly.assertThat(request.getRow()).isEqualTo(1);
        softly.assertThat(request.getSize()).isEqualTo(256);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMalformed() {
        givenARequest("abc", "3", "0", "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenParameterLevelIsMalformed() {
        givenARequest(UUID.randomUUID().toString(), "a", "0", "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenParameterColIsMalformed() {
        givenARequest(UUID.randomUUID().toString(), "3", "a", "1", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenParameterRowIsMalformed() {
        givenARequest(UUID.randomUUID().toString(), "3", "0", "a", "256", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenParameterSizeIsMalformed() {
        givenARequest(UUID.randomUUID().toString(), "3", "0", "1", "a", "true");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMalformed() {
        final var designId = UUID.randomUUID();
        givenARequest(designId.toString(), "3", "0", "1", "256", "a");

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.getLevel()).isEqualTo(3);
        softly.assertThat(request.getCol()).isEqualTo(0);
        softly.assertThat(request.getRow()).isEqualTo(1);
        softly.assertThat(request.getSize()).isEqualTo(256);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    private void givenARequest(String designId, String level, String col, String row, String size, String draft) {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("designId")).thenReturn(designId);
        when(httpRequest.getParam("level")).thenReturn(level);
        when(httpRequest.getParam("col")).thenReturn(col);
        when(httpRequest.getParam("row")).thenReturn(row);
        when(httpRequest.getParam("size")).thenReturn(size);
        when(httpRequest.getParam("draft", "false")).thenReturn(draft != null ? draft : "false");
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(new UUID(0L, 1L), 0, 0, 0, 256, false),
                Arguments.of(new UUID(0L, 1L), 0, 0, 0, 256, true),
                Arguments.of(new UUID(0L, 2L), 0, 0, 0, 512, true),
                Arguments.of(new UUID(0L, 3L), 1, 0, 0, 256, true),
                Arguments.of(new UUID(0L, 4L), 2, 1, 0, 256, true),
                Arguments.of(new UUID(0L, 5L), 3, 2, 1, 256, true)
        );
    }
}