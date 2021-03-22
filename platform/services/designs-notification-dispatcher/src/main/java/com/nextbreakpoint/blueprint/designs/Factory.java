package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import com.nextbreakpoint.blueprint.designs.common.MessaggeFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.MessaggeSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.operations.DesignChangedInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.DesignChangedOutputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class Factory {
    private Factory() {}

    public static Handler<Message> createDesignChangedHandler(Controller<DesignChanged, DesignChanged> controller) {
        return TemplateHandler.<Message, DesignChanged, DesignChanged, JsonObject>builder()
                .withInputMapper(new DesignChangedInputMapper())
                .withController(controller)
                .withOutputMapper(new DesignChangedOutputMapper())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
