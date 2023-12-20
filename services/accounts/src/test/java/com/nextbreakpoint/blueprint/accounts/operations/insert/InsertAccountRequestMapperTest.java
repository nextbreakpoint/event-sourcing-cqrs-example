package com.nextbreakpoint.blueprint.accounts.operations.insert;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RequestBody;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsertAccountRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final RequestBody requestBody = mock();

    private final InsertAccountRequestMapper mapper = new InsertAccountRequestMapper();

    @Test
    void shouldCreateRequest() {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("email", "test@localhost", "name", "test", "role", "admin"));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isNotNull();
        softly.assertThat(request.getName()).isEqualTo("test");
        softly.assertThat(request.getEmail()).isEqualTo("test@localhost");
        softly.assertThat(request.getAuthorities()).isEqualTo("admin");
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
    void shouldThrowExceptionWhenBodyDoesNotContainEmail() {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("name", "test", "role", "admin"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body doesn't contain the required properties: email is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainName() {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("email", "test@localhost", "role", "admin"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body doesn't contain the required properties: name is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainRole() {
        when(context.request()).thenReturn(httpRequest);
        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("email", "test@localhost", "name", "test"));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body doesn't contain the required properties: role is missing");
    }
}