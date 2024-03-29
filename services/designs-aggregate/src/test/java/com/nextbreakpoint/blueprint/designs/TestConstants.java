package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;

import java.util.UUID;

public interface TestConstants {
    String DESIGN_INSERT_REQUESTED = DesignInsertRequested.getClassSchema().getFullName();
    String DESIGN_UPDATE_REQUESTED = DesignUpdateRequested.getClassSchema().getFullName();
    String DESIGN_DELETE_REQUESTED = DesignDeleteRequested.getClassSchema().getFullName();
    String DESIGN_AGGREGATE_UPDATED = DesignAggregateUpdated.getClassSchema().getFullName();
    String TILE_RENDER_REQUESTED = TileRenderRequested.getClassSchema().getFullName();
    String TILE_RENDER_COMPLETED = TileRenderCompleted.getClassSchema().getFullName();
    String DESIGN_DOCUMENT_UPDATE_REQUESTED = DesignDocumentUpdateRequested.getClassSchema().getFullName();
    String DESIGN_DOCUMENT_DELETE_REQUESTED = DesignDocumentDeleteRequested.getClassSchema().getFullName();
    String TILES_RENDERED = TilesRendered.getClassSchema().getFullName();

    String MESSAGE_SOURCE = "service-designs";
    String EVENTS_TOPIC_NAME = "test-designs-aggregate-events";
    String BUFFER_TOPIC_NAME = "test-designs-aggregate-buffer";
    String RENDER_TOPIC_PREFIX = "test-designs-aggregate-render";

    String DATA_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    String DATA_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    String DATA_3 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 50] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    int LEVELS_DRAFT = 3;
    int LEVELS_READY = 8;

    String UUID1_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[1][0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}";
    String UUID6_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    String KEY_REGEXP = TestConstants.UUID6_REGEXP + "/" + TestConstants.UUID6_REGEXP + "/[0-9]+/[0-9]{4}[0-9]{4}";

    String REVISION_REGEXP = "[0-9]{16}-[0-9]{16}";

    String DATABASE_KEYSPACE = "test_designs_aggregate";

    UUID DESIGN_ID_1 = new UUID(0L, 1L);
    UUID DESIGN_ID_2 = new UUID(0L, 2L);
    UUID DESIGN_ID_3 = new UUID(0L, 3L);
    UUID DESIGN_ID_4 = new UUID(0L, 4L);
    UUID DESIGN_ID_5 = new UUID(0L, 5L);

    UUID USER_ID_1 = new UUID(0L, 1L);
    UUID USER_ID_2 = new UUID(0L, 2L);

    String REVISION_0 = "0000000000000000-0000000000000000";
    String REVISION_1 = "0000000000000000-0000000000000001";
    String REVISION_2 = "0000000000000000-0000000000000002";
    String REVISION_3 = "0000000000000000-0000000000000003";

    UUID COMMAND_ID_1 = new UUID(1L, 1L);
    UUID COMMAND_ID_2 = new UUID(1L, 2l);
    UUID COMMAND_ID_3 = new UUID(1L, 3l);
    UUID COMMAND_ID_4 = new UUID(1L, 4l);
    UUID COMMAND_ID_5 = new UUID(1L, 5l);
}
