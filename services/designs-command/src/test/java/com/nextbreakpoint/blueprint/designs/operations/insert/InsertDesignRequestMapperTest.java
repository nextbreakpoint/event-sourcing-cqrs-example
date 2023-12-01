package com.nextbreakpoint.blueprint.designs.operations.insert;

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

class InsertDesignRequestMapperTest {
    private final RoutingContext context = mock();
    private final HttpServerRequest httpRequest = mock();
    private final RequestBody requestBody = mock();
    private final User user = mock();

    private final InsertDesignRequestMapper mapper = new InsertDesignRequestMapper();

    @Test
    void shouldCreateRequest() {
        final UUID userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA, "script", SCRIPT));

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getUuid()).isNotNull();
        softly.assertThat(request.getOwner()).isEqualTo(userId);
        softly.assertThat(request.getChange()).isNotNull();
        softly.assertThat(request.getJson()).isEqualTo("{\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}");
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenBodyIsMissing() {
        final UUID userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(null);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));

        assertThatThrownBy(() -> mapper.transform(context)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainScript() {
        final UUID userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "metadata", METADATA));

        assertThatThrownBy(() -> mapper.transform(context)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainMetadata() {
        final UUID userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("manifest", MANIFEST, "script", SCRIPT));

        assertThatThrownBy(() -> mapper.transform(context)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionWhenBodyDoesNotContainManifest() {
        final UUID userId = UUID.randomUUID();

        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(user);
        when(context.body()).thenReturn(requestBody);
        when(user.principal()).thenReturn(JsonObject.of("user", userId));
        when(requestBody.asJsonObject()).thenReturn(JsonObject.of("script", SCRIPT, "metadata", METADATA));

        assertThatThrownBy(() -> mapper.transform(context)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        when(context.request()).thenReturn(httpRequest);
        when(context.user()).thenReturn(null);

        assertThatThrownBy(() -> mapper.transform(context)).isInstanceOf(IllegalStateException.class);
    }
}