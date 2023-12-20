package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import org.junit.jupiter.api.Test;
import rx.Single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GitHubSignOutHandlerTest {
    private final RoutingContextAdapter routingContext = mock();

    private final GitHubSignOutHandler handler = new GitHubSignOutHandler("localhost");

    @Test
    void shouldHandleRequest() {
        when(routingContext.getSignOutRedirectUrl()).thenReturn("http://host/signout");
        when(routingContext.sendRedirectResponse(any(), any())).thenReturn(Single.just(null));

        handler.handle(routingContext);

        verify(routingContext).getSignOutRedirectUrl();
        verify(routingContext).sendRedirectResponse(eq("http://host/signout"), assertArg(cookie -> {
            assertThat(cookie.getDomain()).isEqualTo("localhost");
            assertThat(cookie.getValue()).isEmpty();
        }));
        verifyNoMoreInteractions(routingContext);
    }

    @Test
    void shouldProduceExceptionWhenRedirectFails() {
        final var exception = new RuntimeException();
        when(routingContext.getSignOutRedirectUrl()).thenReturn("http://host/signout");
        when(routingContext.sendRedirectResponse(any(), any())).thenReturn(Single.error(exception));

        handler.handle(routingContext);

        verify(routingContext).getSignOutRedirectUrl();
        verify(routingContext).sendRedirectResponse(eq("http://host/signout"), assertArg(cookie -> {
            assertThat(cookie.getDomain()).isEqualTo("localhost");
            assertThat(cookie.getValue()).isEmpty();
        }));
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);
    }
}