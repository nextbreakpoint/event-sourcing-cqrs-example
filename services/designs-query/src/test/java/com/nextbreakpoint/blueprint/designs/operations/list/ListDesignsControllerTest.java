package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ListDesignsControllerTest {
    private final Store store = mock();

    private final ListDesignsController controller = new ListDesignsController(store);

    @Test
    void shouldReturnAListOfDesigns() {
        final var design1 = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1);
        final var design2 = aDesign(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, DATA_2);

        final var listRequest = ListDesignsRequest.builder()
                .withDraft(true)
                .withFrom(0)
                .withSize(10)
                .build();

        final var listResponse = ListDesignsResponse.builder()
                .withTotal(2)
                .withDesigns(List.of(design1, design2))
                .build();

        when(store.listDesigns(listRequest)).thenReturn(Single.just(listResponse));

        final var response = controller.onNext(listRequest)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getDesigns()).containsExactlyInAnyOrderElementsOf(List.of(design1, design2));

        verify(store).listDesigns(listRequest);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnAnEmptyList() {
        final var listRequest = ListDesignsRequest.builder()
                .withDraft(true)
                .withFrom(0)
                .withSize(10)
                .build();

        final var listResponse = ListDesignsResponse.builder()
                .withTotal(2)
                .withDesigns(List.of())
                .build();

        when(store.listDesigns(listRequest)).thenReturn(Single.just(listResponse));

        final var response = controller.onNext(listRequest)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getDesigns()).isEmpty();

        verify(store).listDesigns(listRequest);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnAnErrorWhenStoreFails() {
        final var listRequest = ListDesignsRequest.builder()
                .withDraft(true)
                .withFrom(0)
                .withSize(10)
                .build();

        final var exception = new RuntimeException();
        when(store.listDesigns(listRequest)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(listRequest).toCompletable().await()).isEqualTo(exception);

        verify(store).listDesigns(listRequest);
        verifyNoMoreInteractions(store);
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, String revision, String data) {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);
        return TestUtils.aPublishedDesign(designId, commandId, revision, created, updated, data);
    }
}