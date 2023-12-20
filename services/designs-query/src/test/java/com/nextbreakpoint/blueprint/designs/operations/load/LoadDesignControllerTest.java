package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LoadDesignControllerTest {
    private final Store store = mock();

    private final LoadDesignController controller = new LoadDesignController(store);

    @Test
    void shouldReturnADesign() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1);

        final var listRequest = LoadDesignRequest.builder()
                .withDraft(true)
                .withUuid(DESIGN_ID_1)
                .build();

        final var listResponse = LoadDesignResponse.builder()
                .withDesign(design)
                .build();

        when(store.loadDesign(listRequest)).thenReturn(Single.just(listResponse));

        final var response = controller.onNext(listRequest)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();

        assertThat(response.getDesign()).isPresent().hasValue(design);

        verify(store).loadDesign(listRequest);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnAnErrorWhenStoreFails() {
        final var listRequest = LoadDesignRequest.builder()
                .withDraft(true)
                .withUuid(DESIGN_ID_1)
                .build();

        final var exception = new RuntimeException();
        when(store.loadDesign(listRequest)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(listRequest).toCompletable().await()).isEqualTo(exception);

        verify(store).loadDesign(listRequest);
        verifyNoMoreInteractions(store);
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, String revision, String data) {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);
        return TestUtils.aPublishedDesign(designId, commandId, revision, created, updated, data);
    }

}