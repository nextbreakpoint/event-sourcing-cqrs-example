package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.common.vertx.consumer.MessaggeFailureConsumer;
import com.nextbreakpoint.blueprint.common.vertx.consumer.MessaggeSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.controllers.DesignChangedInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.DesignChangedOutputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class Factory {
    private Factory() {}

    public static Handler<Message> createDesignChangedHandler(Controller<DesignChanged, DesignChanged> controller) {
        return TemplateHandler.<Message, DesignChanged, DesignChanged, JsonObject>builder()
                .withInputMapper(new DesignChangedInputMapper())
                .withOutputMapper(new DesignChangedOutputMapper())
                .withController(controller)
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
