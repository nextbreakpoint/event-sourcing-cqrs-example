package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Tracing;

import java.util.UUID;

public interface TestConstants {
    String DESIGN_INSERT_REQUESTED = "design-insert-requested-v1";
    String DESIGN_UPDATE_REQUESTED = "design-update-requested-v1";
    String DESIGN_DELETE_REQUESTED = "design-delete-requested-v1";
    String DESIGN_AGGREGATE_UPDATE_REQUESTED = "design-aggregate-update-requested-v1";
    String DESIGN_AGGREGATE_UPDATE_COMPLETED = "design-aggregate-update-completed-v1";
    String TILE_RENDER_REQUESTED = "tile-render-requested-v1";
    String TILE_RENDER_COMPLETED = "tile-render-completed-v1";
    String TILE_AGGREGATE_UPDATE_REQUIRED = "tile-aggregate-update-required-v1";
    String TILE_AGGREGATE_UPDATE_REQUESTED = "tile-aggregate-update-requested-v1";
    String TILE_AGGREGATE_UPDATE_COMPLETED = "tile-aggregate-update-completed-v1";
    String DESIGN_DOCUMENT_UPDATE_REQUESTED = "design-document-update-requested-v1";
    String DESIGN_DOCUMENT_DELETED_REQUESTED = "design-document-delete-requested-v1";

    String MESSAGE_SOURCE = "service-designs";
    String EVENTS_TOPIC_NAME = "test-designs-aggregate-events";
    String RENDER_TOPIC_NAME = "test-designs-aggregate-render";
    String UPDATE_TOPIC_NAME = "test-designs-aggregate-update";

    String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    String CHECKSUM_1 = Checksum.of(JSON_1);
    String CHECKSUM_2 = Checksum.of(JSON_2);

    int LEVELS = 3;

    String UUID1_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[1][0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}";
    String UUID6_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    String TRACE_ID_REGEXP = ".+";
    String SPAN_ID_REGEXP = ".+";

    String REVISION_REGEXP = "[0-9]{16}-[0-9]{16}";

    String DATABASE_KEYSPACE = "test_designs_aggregate";

    UUID USER_ID = new UUID(0L, 1L);

    String REVISION_0 = "0000000000000000-0000000000000000";
    String REVISION_1 = "0000000000000000-0000000000000001";
    String REVISION_2 = "0000000000000000-0000000000000002";
    String REVISION_3 = "0000000000000000-0000000000000003";
    String REVISION_4 = "0000000000000000-0000000000000004";

    Tracing TRACING = Tracing.of("a", "b");
}
