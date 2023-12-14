package com.nextbreakpoint.blueprint.designs.operations.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.nextbreakpoint.blueprint.common.core.ValidationStatus.ACCEPTED;
import static com.nextbreakpoint.blueprint.common.core.ValidationStatus.REJECTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;

class ValidateDesignControllerTest {
    private final ValidateDesignController controller = new ValidateDesignController();

    @Test
    void shouldHandleRequest() {
        final var request = ValidateDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull().isEqualTo(ACCEPTED);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldReturnErrorForInvalidData() {
        final var request = ValidateDesignRequest.builder()
                .withManifest(INVALID_MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull().isEqualTo(REJECTED);
        assertThat(response.getErrors()).hasSize(1);
    }
}