package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.BlockingHandler;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;

public class Factory {
    private Factory() {}

    public static BlockingHandler<InputMessage> createDesignDocumentUpdateCompletedHandler(Controller<DesignDocumentUpdateCompleted, Void> controller) {
        return TemplateHandler.<InputMessage, DesignDocumentUpdateCompleted, Void, Void>builder()
                .withInputMapper(new DesignDocumentUpdateCompletedInputMapper())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignDocumentDeleteCompletedHandler(Controller<DesignDocumentDeleteCompleted, Void> controller) {
        return TemplateHandler.<InputMessage, DesignDocumentDeleteCompleted, Void, Void>builder()
                .withInputMapper(new DesignDocumentDeleteCompletedInputMapper())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
