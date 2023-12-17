package com.nextbreakpoint.blueprint.designs.operations.upload;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final FileUpload fileUpload = mock();

    private final UploadDesignRequestMapper mapper = new UploadDesignRequestMapper();

    @Test
    void shouldCreateRequest() {
        when(context.request()).thenReturn(httpRequest);
        when(context.fileUploads()).thenReturn(List.of(fileUpload));
        when(fileUpload.uploadedFileName()).thenReturn("filename");

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getFile()).isEqualTo("filename");
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenRequestDoesNotContainUploadedFile() {
        when(context.request()).thenReturn(httpRequest);
        when(context.fileUploads()).thenReturn(List.of());

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("doesn't contain the required file");
    }
}