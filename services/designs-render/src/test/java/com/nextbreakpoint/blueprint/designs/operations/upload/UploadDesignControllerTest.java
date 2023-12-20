package com.nextbreakpoint.blueprint.designs.operations.upload;

import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;

class UploadDesignControllerTest {
    private final UploadDesignController controller = new UploadDesignController();

    @Test
    void shouldHandleRequest() throws Exception {
        final var file = Files.createTempFile("test-", ".nf.zip");

        final var request = UploadDesignRequest.builder()
                .withFile(file.toFile().getAbsolutePath())
                .build();

        final Bundle bundle = BundleUtils.createBundle(MANIFEST, METADATA, SCRIPT).orThrow();

        BundleUtils.writeBundle(bundle, new FileOutputStream(file.toFile())).orThrow();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getManifest()).isNotNull().isEqualTo(MANIFEST);
        assertThat(response.getMetadata()).isNotNull().isEqualTo(METADATA);
        assertThat(response.getScript()).isNotNull().isEqualTo(SCRIPT);
    }

    @Test
    void shouldReturnAnErrorForInvalidData() throws Exception {
        final var file = Files.createTempFile("test-", ".nf.zip");

        final var request = UploadDesignRequest.builder()
                .withFile(file.toFile().getAbsolutePath())
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getManifest()).isNull();
        assertThat(response.getMetadata()).isNull();
        assertThat(response.getScript()).isNull();
    }
}