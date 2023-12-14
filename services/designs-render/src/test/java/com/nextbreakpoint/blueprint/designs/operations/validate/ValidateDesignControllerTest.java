package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.designs.common.BundleValidator;
import com.nextbreakpoint.blueprint.designs.common.BundleValidatorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nextbreakpoint.blueprint.common.core.ValidationStatus.ACCEPTED;
import static com.nextbreakpoint.blueprint.common.core.ValidationStatus.REJECTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ValidateDesignControllerTest {
    private final BundleValidator validator = mock();

    private final ValidateDesignController controller = new ValidateDesignController(validator);

    @Test
    void shouldHandleRequest() throws BundleValidatorException {
        final var request = ValidateDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull().isEqualTo(ACCEPTED);
        assertThat(response.getErrors()).isEmpty();

        verify(validator).parseAndCompile(MANIFEST, METADATA, SCRIPT);
    }

    @Test
    void shouldReturnAnErrorWhenThereIsAValidationError() throws BundleValidatorException {
        doThrow(new BundleValidatorException("error", List.of("some error"))).when(validator).parseAndCompile(any(), any(), any());

        final var request = ValidateDesignRequest.builder()
                .withManifest(MANIFEST)
                .withMetadata(METADATA)
                .withScript(SCRIPT)
                .build();

        final var response = controller.onNext(request).doOnError(Assertions::fail).toBlocking().value();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull().isEqualTo(REJECTED);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).isEqualTo("some error");

        verify(validator).parseAndCompile(MANIFEST, METADATA, SCRIPT);
    }
}