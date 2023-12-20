package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.OAuthAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.auth.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CallbackHandlerTest {
    private final User user = mock();
    private final OAuthAdapter oauthAdapter = mock();
    private final RoutingContextAdapter routingContext = mock();

    private final CallbackHandler handler = new CallbackHandler(oauthAdapter);

    @Test
    void shouldHandleRequest() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn("123");
        when(routingContext.getRequestParam("state")).thenReturn("/some/resource");

        handler.handle(routingContext);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).getRequestParam("state");
        verifyNoMoreInteractions(routingContext);

        verify(oauthAdapter).authenticate(eq("123"), any());
    }

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldFailWhenErrorParameterIsPresent(int code, String error, String errorDescription) {
        when(routingContext.getRequestParam("error")).thenReturn(error);
        when(routingContext.getRequestParam("error_description")).thenReturn(errorDescription);

        handler.handle(routingContext);

        verify(routingContext, times(2)).getRequestParam("error");
        verify(routingContext).getRequestParam("error_description");
        verify(routingContext).fail(eq(code), assertArg(exception -> {
            assertThat(exception).isInstanceOf(IllegalStateException.class);
            if (errorDescription != null) {
                assertThat(exception).hasMessageContaining("Authentication error: " + error + ". some error description");
            } else {
                assertThat(exception).hasMessageContaining("Authentication error: " + error);
            }
        }));
        verifyNoMoreInteractions(routingContext);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldFailWhenCodeParameterIsMissing() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn(null);

        handler.handle(routingContext);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).fail(eq(400), assertArg(error -> {
            assertThat(error).isInstanceOf(IllegalStateException.class).hasMessageContaining("Missing code parameter");
        }));
        verifyNoMoreInteractions(routingContext);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldFailWhenStateParameterIsMissing() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn("123");
        when(routingContext.getRequestParam("state")).thenReturn(null);

        handler.handle(routingContext);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).getRequestParam("state");
        verify(routingContext).fail(eq(400), assertArg(error -> {
            assertThat(error).isInstanceOf(IllegalStateException.class).hasMessageContaining("Missing state parameter");
        }));
        verifyNoMoreInteractions(routingContext);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldHandleAuthenticationError() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn("123");
        when(routingContext.getRequestParam("state")).thenReturn("/some/resource");

        handler.handle(routingContext);

        final ArgumentCaptor<Handler<AsyncResult<User>>> handlerCapture = ArgumentCaptor.forClass(Handler.class);
        verify(oauthAdapter).authenticate(eq("123"), handlerCapture.capture());
        final var handler = handlerCapture.getValue();

        final var exception = new RuntimeException();
        final AsyncResult<User> result = Future.failedFuture(exception);
        handler.handle(result);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).getRequestParam("state");
        verify(routingContext).fail(eq(500), assertArg(error -> assertThat(error).isEqualTo(exception)));
        verifyNoMoreInteractions(routingContext);
    }

    @Test
    void shouldHandleAuthenticationReroute() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn("123");
        when(routingContext.getRequestParam("state")).thenReturn("/some/resource");

        handler.handle(routingContext);

        final ArgumentCaptor<Handler<AsyncResult<User>>> handlerCapture = ArgumentCaptor.forClass(Handler.class);
        verify(oauthAdapter).authenticate(eq("123"), handlerCapture.capture());
        final var handler = handlerCapture.getValue();

        final AsyncResult<User> result = Future.succeededFuture(user);
        handler.handle(result);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).getRequestParam("state");
        verify(routingContext).setUser(user);
        verify(routingContext).reroute("/some/resource");
        verifyNoMoreInteractions(routingContext);
    }

    @Test
    void shouldHandleAuthenticationRedirect() {
        when(routingContext.getRequestParam("error")).thenReturn(null);
        when(routingContext.getRequestParam("code")).thenReturn("123");
        when(routingContext.getRequestParam("state")).thenReturn("http://host/some/resource");

        handler.handle(routingContext);

        final ArgumentCaptor<Handler<AsyncResult<User>>> handlerCapture = ArgumentCaptor.forClass(Handler.class);
        verify(oauthAdapter).authenticate(eq("123"), handlerCapture.capture());
        final var handler = handlerCapture.getValue();

        final AsyncResult<User> result = Future.succeededFuture(user);
        handler.handle(result);

        verify(routingContext).getRequestParam("error");
        verify(routingContext).getRequestParam("code");
        verify(routingContext).getRequestParam("state");
        verify(routingContext).setUser(user);
        verify(routingContext).sendRedirectResponse("http://host/some/resource");
        verifyNoMoreInteractions(routingContext);
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
            Arguments.of(401, "invalid_token", "some error description"),
            Arguments.of(403, "insufficient_scope", "some error description"),
            Arguments.of(400, "invalid_request", "some error description"),
            Arguments.of(400, "other", "some error description"),
            Arguments.of(401, "invalid_token", null),
            Arguments.of(403, "insufficient_scope", null),
            Arguments.of(400, "invalid_request", null),
            Arguments.of(400, "other", null)
        );
    }
}