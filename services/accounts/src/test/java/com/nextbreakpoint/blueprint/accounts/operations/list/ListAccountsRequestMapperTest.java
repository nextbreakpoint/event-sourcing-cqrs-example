package com.nextbreakpoint.blueprint.accounts.operations.list;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListAccountsRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();

    private final ListAccountsRequestMapper mapper = new ListAccountsRequestMapper();

    @Test
    void shouldCreateRequest() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("email")).thenReturn("test@localhost");

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getEmail()).isPresent().hasValue("test@localhost");
        softly.assertAll();
    }

    @Test
    void shouldNotReturnAnEmailWhenEmailIsMissing() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("email")).thenReturn(null);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getEmail()).isNotPresent();
        softly.assertAll();
    }
}