package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.accounts.model.Account;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadAccountControllerTest {
    private final Store store = mock();
    private final LoadAccountController controller = new LoadAccountController(store);

    @Test
    void shouldHandleRequest() {
        final UUID uuid = UUID.randomUUID();

        final var account = Account.builder()
                .withUuid(uuid.toString())
                .withName("test")
                .withAuthorities("admin")
                .build();

        final var request = LoadAccountRequest.builder()
                .withUuid(uuid)
                .build();

        final var response = LoadAccountResponse.builder()
                .withUuid(uuid)
                .withAccount(account)
                .build();

        when(store.loadAccount(request)).thenReturn(Single.just(response));

        final var actualResponse = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualResponse.getUuid()).isEqualTo(uuid);
        softly.assertThat(actualResponse.getAccount()).isPresent().hasValue(account);
        softly.assertAll();
    }

    @Test
    void shouldReturnErrorWhenStoreFails() {
        final UUID uuid = UUID.randomUUID();

        final var request = LoadAccountRequest.builder()
                .withUuid(uuid)
                .build();

        final var exception = new RuntimeException("some error");
        when(store.loadAccount(request)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(request).toCompletable().await()).isEqualTo(exception);
    }
}