package com.nextbreakpoint.blueprint.designs.operations.update;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RequestBody;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final RequestBody requestBody = mock();
    private final User user = mock();

    private final UpdateDesignRequestMapper mapper = new UpdateDesignRequestMapper();

    @Test
    void shouldCreateRequest() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA, "script", SCRIPT, "published", true));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isNotNull();
        softly.assertThat(request.getOwner()).isEqualTo(userId);
        softly.assertThat(request.getChange()).isNotNull();
        softly.assertThat(request.getPublished()).isEqualTo(true);
        softly.assertThat(request.getJson()).isEqualTo("{\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}");
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMissing() {
        when(context.request()).thenReturn(httpRequest);
        when(httpRequest.getParam("designId")).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("designId is missing");
    }

    @Test
    void shouldThrowExceptionWhenParameterDesignIdIsMalformed() {
        final var userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(httpRequest.getParam("designId")).thenReturn("abc");
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA, "script", SCRIPT, "published", true));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("invalid request");
    }

    @Test
    void shouldThrowExceptionWhenBodyIsEmpty() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(null);
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(user.principal()).thenReturn(JsonObject.of("user", userId));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body is empty");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainScript() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: script is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainMetadata() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "script", SCRIPT));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: metadata is missing");
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainManifest() {
        final var userId = UUID.randomUUID();
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("script", SCRIPT, "metadata", METADATA));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contain the required properties: manifest is missing");
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(null);
        when(context.body()).thenReturn(requestBody);
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA, "script", SCRIPT, "published", true));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("user is not authenticated");
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMalformed() {
        final var designId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", "abc"));
        when(httpRequest.getParam("designId")).thenReturn(designId.toString());
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA, "script", SCRIPT, "published", true));

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid request");
    }
}