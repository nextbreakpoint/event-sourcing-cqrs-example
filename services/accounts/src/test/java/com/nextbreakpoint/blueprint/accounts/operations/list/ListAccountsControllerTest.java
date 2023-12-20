package com.nextbreakpoint.blueprint.accounts.operations.list;

import com.nextbreakpoint.blueprint.accounts.Store;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListAccountsControllerTest {
    private final Store store = mock();
    private final ListAccountsController controller = new ListAccountsController(store);

    @Test
    void shouldHandleRequest() {
        final List<String> accounts = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final var request = ListAccountsRequest.builder()
                .withEmail("test@localhost")
                .build();

        final var response = ListAccountsResponse.builder()
                .withUuids(accounts)
                .build();

        when(store.listAccounts(request)).thenReturn(Single.just(response));

        final var actualResponse = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualResponse.getUuids()).containsExactlyInAnyOrderElementsOf(accounts);
        softly.assertAll();
    }

    @Test
    void shouldReturnErrorWhenStoreFails() {
        final var request = ListAccountsRequest.builder()
                .withEmail("test@localhost")
                .build();

        final var exception = new RuntimeException("some error");
        when(store.listAccounts(request)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(request).toCompletable().await()).isEqualTo(exception);
    }
}