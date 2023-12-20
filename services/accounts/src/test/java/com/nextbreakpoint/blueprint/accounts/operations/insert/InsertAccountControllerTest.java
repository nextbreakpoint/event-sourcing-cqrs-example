package com.nextbreakpoint.blueprint.accounts.operations.insert;

import com.nextbreakpoint.blueprint.accounts.Store;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsertAccountControllerTest {
    private final Store store = mock();
    private final InsertAccountController controller = new InsertAccountController(store);

    @Test
    void shouldHandleRequest() {
        final UUID uuid = UUID.randomUUID();

        final var request = InsertAccountRequest.builder()
                .withUuid(uuid)
                .withName("test")
                .withEmail("test@localhost")
                .withAuthorities("admin")
                .build();

        final var response = InsertAccountResponse.builder()
                .withUuid(uuid)
                .withAuthorities("admin")
                .withResult(1)
                .build();

        when(store.insertAccount(request)).thenReturn(Single.just(response));

        final var actualResponse = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualResponse.getUuid()).isEqualTo(uuid);
        softly.assertThat(actualResponse.getAuthorities()).isEqualTo("admin");
        softly.assertThat(actualResponse.getResult()).isEqualTo(1);
        softly.assertAll();
    }

    @Test
    void shouldReturnErrorWhenStoreFails() {
        final UUID uuid = UUID.randomUUID();

        final var request = InsertAccountRequest.builder()
                .withUuid(uuid)
                .withName("test")
                .withEmail("test@localhost")
                .withAuthorities("admin")
                .build();

        final var exception = new RuntimeException("some error");
        when(store.insertAccount(request)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(request).toCompletable().await()).isEqualTo(exception);
    }
}