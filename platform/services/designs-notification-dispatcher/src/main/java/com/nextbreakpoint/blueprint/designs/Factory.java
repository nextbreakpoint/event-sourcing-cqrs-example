package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.BlockingHandler;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdateCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileAggregateUpdateCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailureConsumer;
import com.nextbreakpoint.blueprint.common.vertx.MessageSuccessConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;

public class Factory {
    private Factory() {}

    public static BlockingHandler<InputMessage> createDesignAggregateUpdateCompletedHandler(Controller<DesignAggregateUpdateCompleted, Void> controller) {
        return TemplateHandler.<InputMessage, DesignAggregateUpdateCompleted, Void, Void>builder()
                .withInputMapper(new DesignAggregateUpdateCompletedInputMapper())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileAggregateUpdateCompletedHandler(Controller<TileAggregateUpdateCompleted, Void> controller) {
        return TemplateHandler.<InputMessage, TileAggregateUpdateCompleted, Void, Void>builder()
                .withInputMapper(new TileAggregateUpdateCompletedInputMapper())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }
}
