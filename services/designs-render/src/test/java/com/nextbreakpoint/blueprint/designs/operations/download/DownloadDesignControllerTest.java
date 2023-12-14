package com.nextbreakpoint.blueprint.designs.operations.download;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;

class DownloadDesignControllerTest {
    private final DownloadDesignController controller = new DownloadDesignController();

    @Test
    void shouldHandleRequest() {
        final var request = DownloadDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getBytes()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldReturnErrorForInvalidData() {
        final var request = DownloadDesignRequest.builder()
                .withManifest(INVALID_MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getBytes()).isNotNull().isEmpty();
    }
}