package com.nextbreakpoint.blueprint.designs.operations.delete;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeleteDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final User user = mock();

    private final DeleteDesignRequestMapper mapper = new DeleteDesignRequestMapper();

    @Test
    void shouldCreateRequest() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(user.principal()).thenReturn(JsonObject.of("user", userId));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(designId);
        softly.assertThat(request.getOwner()).isEqualTo(userId);
        softly.assertThat(request.getChange()).isNotNull();
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMissing() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("designId")).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("designId is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMalformed() {
        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(httpRequest.getParam("designId")).thenReturn("abc");
        when(user.principal()).thenReturn(JsonObject.of("user", "abc"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(null);
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("user is not authenticated");
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMalformed() {
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(user.principal()).thenReturn(JsonObject.of("user", "abc"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid request");
    }
}