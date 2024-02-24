package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM;
import static com.nextbreakpoint.blueprint.designs.TestUtils.createCacheKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GetImageControllerTest {
    private final S3Driver driver = mock();

    private final GetImageController controller = new GetImageController(driver);

    @Test
    void shouldReturnCachedImageWhenImageExists() {
        final String key = createCacheKey(CHECKSUM);

        when(driver.getObject(key)).thenReturn(Single.just(new byte[] { 0, 1, 2 }));

        final var request = GetImageRequest.builder()
                .withChecksum(CHECKSUM)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getImage()).hasValue(new Image(new byte[] { 0, 1, 2 }, CHECKSUM));

        verify(driver).getObject(key);
        verifyNoMoreInteractions(driver);
    }

    @Test
    void shouldReturnEmptyImageWhenImageDoesNotExist() {
        final String key = createCacheKey(CHECKSUM);

        when(driver.getObject(key)).thenReturn(Single.error(new RuntimeException()));

        final var request = GetImageRequest.builder()
                .withChecksum(CHECKSUM)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getImage()).isNotPresent();

        verify(driver).getObject(key);
        verifyNoMoreInteractions(driver);
    }
}