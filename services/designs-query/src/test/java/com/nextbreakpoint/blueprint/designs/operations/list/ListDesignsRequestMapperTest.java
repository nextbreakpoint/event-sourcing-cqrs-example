package com.nextbreakpoint.blueprint.designs.operations.list;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListDesignsRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();

    private final ListDesignsRequestMapper mapper = new ListDesignsRequestMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateRequest(int from, int size, boolean draft) {
        givenARequest(String.valueOf(from), String.valueOf(size), String.valueOf(draft));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFrom()).isEqualTo(from);
        softly.assertThat(request.getSize()).isEqualTo(size);
        softly.assertThat(request.isDraft()).isEqualTo(draft);
        softly.assertAll();
    }

    @Test
    void shouldReturnDefaultValueWhenParameterFromIsMissing() {
        givenARequest(null, String.valueOf(50), String.valueOf(true));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFrom()).isEqualTo(0);
        softly.assertThat(request.getSize()).isEqualTo(50);
        softly.assertThat(request.isDraft()).isEqualTo(true);
        softly.assertAll();
    }

    @Test
    void shouldReturnDefaultValueWhenParameterSizeIsMissing() {
        givenARequest(String.valueOf(100), null, String.valueOf(true));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFrom()).isEqualTo(100);
        softly.assertThat(request.getSize()).isEqualTo(20);
        softly.assertThat(request.isDraft()).isEqualTo(true);
        softly.assertAll();
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMissing() {
        givenARequest(String.valueOf(100), String.valueOf(50), null);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFrom()).isEqualTo(100);
        softly.assertThat(request.getSize()).isEqualTo(50);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterFromIsMalformed() {
        givenARequest("a", String.valueOf(50), String.valueOf(true));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenParameterSizeIsMalformed() {
        givenARequest(String.valueOf(100), "a", String.valueOf(true));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldReturnDefaultValueWhenParameterDraftIsMalformed() {
        givenARequest(String.valueOf(100), String.valueOf(50), "a");

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFrom()).isEqualTo(100);
        softly.assertThat(request.getSize()).isEqualTo(50);
        softly.assertThat(request.isDraft()).isEqualTo(false);
        softly.assertAll();
    }

    private void givenARequest(String from, String size, String draft) {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("from", "0")).thenReturn(from != null ? from : "0");
        when(httpRequest.getParam("size", "20")).thenReturn(size != null ? size : "20");
        when(httpRequest.getParam("draft", "false")).thenReturn(draft != null ? draft : "false");
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(0, 20, false),
                Arguments.of(0, 50, false),
                Arguments.of(0, 100, false),
                Arguments.of(100, 50, false),
                Arguments.of(200, 50, false),
                Arguments.of(200, 50, true)
        );
    }
}