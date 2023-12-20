package com.nextbreakpoint.blueprint.accounts.operations.load;

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

class LoadSelfAccountRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final User user = mock();

    private final LoadSelfAccountRequestMapper mapper = new LoadSelfAccountRequestMapper();

    @Test
    void shouldCreateRequest() {
        final var accountId = UUID.randomUUID();
        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(user.principal()).thenReturn(JsonObject.of("user", accountId.toString()));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(accountId);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("user is not authenticated");
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMalformed() {
        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(user.principal()).thenReturn(JsonObject.of("user", "abc"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid request");
    }
}