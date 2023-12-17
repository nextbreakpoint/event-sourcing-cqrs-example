package com.nextbreakpoint.blueprint.accounts.operations.delete;

import com.nextbreakpoint.blueprint.accounts.Store;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeleteAccountControllerTest {
    private final Store store = mock();
    private final DeleteAccountController controller = new DeleteAccountController(store);

    @Test
    void shouldHandleRequest() {
        final UUID uuid = UUID.randomUUID();

        final var request = DeleteAccountRequest.builder()
                .withUuid(uuid)
                .build();

        final var response = DeleteAccountResponse.builder()
                .withUuid(uuid)
                .withResult(1)
                .build();

        when(store.deleteAccount(request)).thenReturn(Single.just(response));

        final var actualResponse = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualResponse.getUuid()).isEqualTo(uuid);
        softly.assertThat(actualResponse.getResult()).isEqualTo(1);
        softly.assertAll();
    }

    @Test
    void shouldReturnErrorWhenStoreFails() {
        final UUID uuid = UUID.randomUUID();

        final var request = DeleteAccountRequest.builder()
                .withUuid(uuid)
                .build();

        final var exception = new RuntimeException("some error");
        when(store.deleteAccount(request)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(request).toCompletable().await()).isEqualTo(exception);
    }
}