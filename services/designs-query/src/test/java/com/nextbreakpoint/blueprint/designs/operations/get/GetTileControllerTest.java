package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import rx.Single;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GetTileControllerTest {
    private final Store store = mock();
    private final S3Driver driver = mock();

    private final GetTileController controller = new GetTileController(store, driver);

    @ParameterizedTest
    @MethodSource("someRequests")
    void shouldReturnAnImage(String data, GetTileRequest request) {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, data);

        final var loadRequest = LoadDesignRequest.builder()
                .withUuid(request.getUuid())
                .withDraft(request.isDraft())
                .build();

        final var loadResponse = LoadDesignResponse.builder()
                .withDesign(design)
                .build();

        final var key = String.format("%s/%d/%04d%04d.png", design.getChecksum(), request.getLevel(), request.getRow(), request.getCol());

        when(store.loadDesign(loadRequest)).thenReturn(Single.just(loadResponse));
        when(driver.getObject(key)).thenReturn(Single.just(new byte[]{0, 1, 2}));

        final var response = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();

        final var expectedImage = Image.builder()
                .withData(new byte[]{0, 1, 2})
                .withChecksum(design.getChecksum())
                .build();

        assertThat(response.getImage()).isPresent().hasValue(expectedImage);

        verify(store).loadDesign(loadRequest);
        verify(driver).getObject(key);
        verifyNoMoreInteractions(store, driver);
    }

    @Test
    void shouldNotReturnAnImageWhenTileIsNotFound() {
        final var request = aGetTileRequest(DESIGN_ID_1, 1, 0, 0, true);

        final var loadRequest = LoadDesignRequest.builder()
                .withUuid(request.getUuid())
                .withDraft(request.isDraft())
                .build();

        final var loadResponse = LoadDesignResponse.builder().build();

        when(store.loadDesign(loadRequest)).thenReturn(Single.just(loadResponse));

        final var response = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();

        assertThat(response.getImage()).isNotPresent();

        verify(store).loadDesign(loadRequest);
        verifyNoMoreInteractions(store);

        verifyNoInteractions(driver);
    }

    @Test
    void shouldReturnErrorWhenStoreFails() {
        final var request = aGetTileRequest(DESIGN_ID_1, 1, 0, 0, true);

        final var loadRequest = LoadDesignRequest.builder()
                .withUuid(request.getUuid())
                .withDraft(request.isDraft())
                .build();

        final var exception = new RuntimeException("Some error");
        when(store.loadDesign(loadRequest)).thenReturn(Single.error(exception));

        final var response = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getImage()).isNotPresent();

        verify(store).loadDesign(loadRequest);
        verifyNoMoreInteractions(store);

        verifyNoInteractions(driver);
    }

    @Test
    void shouldReturnErrorWhenDriverFails() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1);

        final var request = aGetTileRequest(DESIGN_ID_1, 1, 0, 0, true);

        final var loadRequest = LoadDesignRequest.builder()
                .withUuid(request.getUuid())
                .withDraft(request.isDraft())
                .build();

        final var loadResponse = LoadDesignResponse.builder()
                .withDesign(design)
                .build();

        final var key = String.format("%s/%d/%04d%04d.png", design.getChecksum(), request.getLevel(), request.getRow(), request.getCol());

        when(store.loadDesign(loadRequest)).thenReturn(Single.just(loadResponse));

        final var exception = new RuntimeException("Some error");
        when(driver.getObject(key)).thenReturn(Single.error(exception));

        final var response = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getImage()).isNotPresent();

        verify(store).loadDesign(loadRequest);
        verifyNoMoreInteractions(store);

        verify(driver).getObject(key);
        verifyNoMoreInteractions(driver);
    }

    @NotNull
    private static GetTileRequest aGetTileRequest(UUID designId, int level, int col, int row, boolean draft) {
        return GetTileRequest.builder()
                .withUuid(designId)
                .withLevel(level)
                .withCol(col)
                .withRow(row)
                .withSize(256)
                .withDraft(draft)
                .build();
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, String revision, String data) {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);
        return TestUtils.aPublishedDesign(designId, commandId, revision, created, updated, data);
    }

    private static Stream<Arguments> someRequests() {
        return Stream.of(
                Arguments.of(
                        DATA_1, aGetTileRequest(DESIGN_ID_1, 1, 0, 0, true)
                ),
                Arguments.of(
                        DATA_2, aGetTileRequest(DESIGN_ID_2, 2, 0, 0, true)
                ),
                Arguments.of(
                        DATA_2, aGetTileRequest(DESIGN_ID_2, 2, 3, 4, true)
                ),
                Arguments.of(
                        DATA_2, aGetTileRequest(DESIGN_ID_2, 2, 4, 4, false)
                )
        );
    }
}