package com.nextbreakpoint.blueprint.accounts.operations.delete;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeleteAccountRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();

    private final DeleteAccountRequestMapper mapper = new DeleteAccountRequestMapper();

    @Test
    void shouldCreateRequest() {
        final var accountId = UUID.randomUUID();
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("accountId")).thenReturn(accountId.toString());

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isEqualTo(accountId);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenAccountIdIsMissing() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("accountId")).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accountId is missing");
    }

    @Test
    void shouldThrowExceptionWhenAccountIdIsMalformed() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("accountId")).thenReturn("abc");

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid request");
    }
}