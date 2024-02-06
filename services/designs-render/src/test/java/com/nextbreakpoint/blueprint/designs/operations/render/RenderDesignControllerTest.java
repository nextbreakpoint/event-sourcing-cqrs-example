package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM;
import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static com.nextbreakpoint.blueprint.designs.TestUtils.getCacheKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RenderDesignControllerTest {
    private final S3Driver driver = mock();

    private final RenderDesignController controller = new RenderDesignController(driver);

    @Test
    void shouldCacheImageWhenImageDoesNotExist() {
        final String key = getCacheKey(CHECKSUM);

        when(driver.getObject(key)).thenReturn(Single.error(new RuntimeException()));
        when(driver.putObject(eq(key), any())).thenReturn(Single.just(null));

        final var request = RenderDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getChecksum()).isEqualTo(CHECKSUM);

        verify(driver).getObject(key);
        verify(driver).putObject(eq(key), any());
        verifyNoMoreInteractions(driver);
    }

    @Test
    void shouldReturnCachedImageWhenImageAlreadyExists() {
        final String key = getCacheKey(CHECKSUM);

        when(driver.getObject(key)).thenReturn(Single.just(new byte[] { 0, 1, 2 }));

        final var request = RenderDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getChecksum()).isEqualTo(CHECKSUM);

        verify(driver).getObject(key);
        verifyNoMoreInteractions(driver);
    }

    @Test
    void shouldReturnAnErrorWhenDataIsInvalid() {
        final var request = RenderDesignRequest.builder()
                .withManifest(INVALID_MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getChecksum()).isNull();

        verifyNoInteractions(driver);
    }

    @Test
    void shouldReturnAnErrorWhenImageCannotBeSaved() {
        final String key = getCacheKey(CHECKSUM);

        when(driver.getObject(key)).thenReturn(Single.error(new RuntimeException()));
        when(driver.putObject(eq(key), any())).thenReturn(Single.error(new RuntimeException()));

        final var request = RenderDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getChecksum()).isEqualTo(CHECKSUM);

        verify(driver).getObject(key);
        verify(driver).putObject(eq(key), any());
        verifyNoMoreInteractions(driver);
    }
}