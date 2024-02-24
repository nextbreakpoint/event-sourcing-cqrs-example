package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.AccountsClient;
import com.nextbreakpoint.blueprint.authentication.common.GitHubClient;
import com.nextbreakpoint.blueprint.authentication.common.OAuthAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.authentication.common.TokenProvider;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.List;

import static com.nextbreakpoint.blueprint.common.vertx.Authentication.NULL_USER_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GitHubSignInHandlerTest {
    private final GitHubClient githubClient = mock();
    private final AccountsClient accountsClient = mock();
    private final TokenProvider tokenProvider = mock();
    private final OAuthAdapter oauthAdapter = mock();
    private final RoutingContextAdapter routingContext = mock();

    private final GitHubSignInHandler handler = new GitHubSignInHandler("localhost", githubClient, accountsClient, tokenProvider, oauthAdapter);

    @Test
    void shouldHandleRequestWhenUserIsNotAuthenticated() {
        when(routingContext.isUserAuthenticated()).thenReturn(false);
        when(routingContext.getRequestUri()).thenReturn("http://localhost/resource");
        when(routingContext.sendRedirectResponse(any())).thenReturn(Single.just(null));
        when(oauthAdapter.authorizeURL(any())).thenReturn("http://host/oauth");

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getRequestUri();
        verify(routingContext).sendRedirectResponse("http://host/oauth");
        verifyNoMoreInteractions(routingContext);

        verify(oauthAdapter).authorizeURL("http://localhost/resource");
        verifyNoMoreInteractions(oauthAdapter);

        verifyNoInteractions(tokenProvider, githubClient, accountsClient);
    }

    @Test
    void shouldProduceExceptionWhenUserIsNotAuthenticatedAndRedirectFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(false);
        when(routingContext.getRequestUri()).thenReturn("http://localhost/resource");
        when(routingContext.sendRedirectResponse(any())).thenReturn(Single.error(exception));
        when(oauthAdapter.authorizeURL(any())).thenReturn("http://host/oauth");

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getRequestUri();
        verify(routingContext).sendRedirectResponse("http://host/oauth");
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(oauthAdapter).authorizeURL("http://localhost/resource");
        verifyNoMoreInteractions(oauthAdapter);

        verifyNoInteractions(tokenProvider, githubClient, accountsClient);
    }

    @Test
    void shouldHandleRequestWhenUserIsAuthenticated() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.just(null));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(tokenProvider.generateToken("123456", List.of("admin"))).thenReturn(Single.just("cba"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray().add("123456")));
        when(accountsClient.fetchAccount("efg", "123456")).thenReturn(Single.just(new JsonObject().put("uuid", "123456").put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).sendRedirectResponse(eq("http://localhost/signin"), any());
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verify(tokenProvider).generateToken("123456", List.of("admin"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).fetchAccount("efg", "123456");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndRedirectFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.error(exception));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(tokenProvider.generateToken("123456", List.of("admin"))).thenReturn(Single.just("cba"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray().add("123456")));
        when(accountsClient.fetchAccount("efg", "123456")).thenReturn(Single.just(new JsonObject().put("uuid", "123456").put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).sendRedirectResponse(eq("http://localhost/signin"), any());
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verify(tokenProvider).generateToken("123456", List.of("admin"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).fetchAccount("efg", "123456");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndFetchUserInfoFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.error(exception));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verifyNoInteractions(oauthAdapter, accountsClient);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndFindAccountsFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.error(exception));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndFetchAccountFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray().add("123456")));
        when(accountsClient.fetchAccount("efg", "123456")).thenReturn(Single.error(exception));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).fetchAccount("efg", "123456");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenAccessTokenIsMissing() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn(null);

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(assertArg(error -> assertThat(error.getMessage()).isEqualTo("Access denied: Missing OAuth access token")));
        verifyNoMoreInteractions(routingContext);

        verifyNoInteractions(oauthAdapter, tokenProvider, githubClient, accountsClient);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedButAccountIsMissingUuid() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.just(null));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray().add("123456")));
        when(accountsClient.fetchAccount("efg", "123456")).thenReturn(Single.just(new JsonObject().put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(assertArg(error -> assertThat(error.getMessage()).isEqualTo("Access denied: Missing account's uuid or role")));
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).fetchAccount("efg", "123456");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedButAccountIsMissingRole() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.just(null));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray().add("123456")));
        when(accountsClient.fetchAccount("efg", "123456")).thenReturn(Single.just(new JsonObject().put("uuid", "123456")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(assertArg(error -> assertThat(error.getMessage()).isEqualTo("Access denied: Missing account's uuid or role")));
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).fetchAccount("efg", "123456");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldHandleRequestWhenUserIsAuthenticatedAndAccountDoesNotExist() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.just(null));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(tokenProvider.generateToken("345678", List.of("admin"))).thenReturn(Single.just("cba"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray()));
        when(accountsClient.createAccount("efg", "test-login", "test")).thenReturn(Single.just(new JsonObject().put("uuid", "345678").put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).sendRedirectResponse(eq("http://localhost/signin"), any());
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verify(tokenProvider).generateToken("345678", List.of("admin"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).createAccount("efg", "test-login", "test");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldHandleRequestWhenUserIsAuthenticatedAndAccountDoesNotExistAndUserNameIsMissing() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(routingContext.sendRedirectResponse(eq("http://localhost/signin"), any())).thenReturn(Single.just(null));
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(tokenProvider.generateToken("345678", List.of("admin"))).thenReturn(Single.just("cba"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray()));
        when(accountsClient.createAccount("efg", "test-login", "test-login")).thenReturn(Single.just(new JsonObject().put("uuid", "345678").put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).sendRedirectResponse(eq("http://localhost/signin"), any());
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verify(tokenProvider).generateToken("345678", List.of("admin"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).createAccount("efg", "test-login", "test-login");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndAccountDoesNotExistAndCreateAccountFails() {
        final var exception = new RuntimeException();
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray()));
        when(accountsClient.createAccount("efg", "test-login", "test")).thenReturn(Single.error(exception));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(exception);
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).createAccount("efg", "test-login", "test");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndAccountDoesNotExistAndAccountIsMissingUuid() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray()));
        when(accountsClient.createAccount("efg", "test-login", "test")).thenReturn(Single.just(new JsonObject().put("role", "admin")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(assertArg(error -> assertThat(error.getMessage()).isEqualTo("Access denied: Missing account's uuid or role")));
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).createAccount("efg", "test-login", "test");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }

    @Test
    void shouldProduceExceptionWhenUserIsAuthenticatedAndAccountDoesNotExistAndAccountIsMissingRole() {
        when(routingContext.isUserAuthenticated()).thenReturn(true);
        when(routingContext.getSignInRedirectUrl()).thenReturn("http://localhost/signin");
        when(routingContext.getAccessToken()).thenReturn("abc");
        when(tokenProvider.generateToken(NULL_USER_UUID, List.of("platform"))).thenReturn(Single.just("efg"));
        when(githubClient.fetchUserInfo("abc")).thenReturn(Single.just(new JsonObject().put("name", "test").put("login", "test-login")));
        when(accountsClient.findAccounts("efg", "test-login")).thenReturn(Single.just(new JsonArray()));
        when(accountsClient.createAccount("efg", "test-login", "test")).thenReturn(Single.just(new JsonObject().put("uuid", "345678")));

        handler.handle(routingContext);

        verify(routingContext).isUserAuthenticated();
        verify(routingContext).getSignInRedirectUrl();
        verify(routingContext).getAccessToken();
        verify(routingContext).handleException(assertArg(error -> assertThat(error.getMessage()).isEqualTo("Access denied: Missing account's uuid or role")));
        verifyNoMoreInteractions(routingContext);

        verify(tokenProvider).generateToken(NULL_USER_UUID, List.of("platform"));
        verifyNoMoreInteractions(tokenProvider);

        verify(githubClient).fetchUserInfo("abc");
        verifyNoMoreInteractions(githubClient);

        verify(accountsClient).findAccounts("efg", "test-login");
        verify(accountsClient).createAccount("efg", "test-login", "test");
        verifyNoMoreInteractions(accountsClient);

        verifyNoInteractions(oauthAdapter);
    }
}