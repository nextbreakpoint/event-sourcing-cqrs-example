package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;

public class Factory {
    private Factory() {}

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentUpdateCompletedHandler(Controller<DesignDocumentUpdateCompleted, Void> controller) {
        return TemplateHandler.<InputMessage<Object>, DesignDocumentUpdateCompleted, Void, Void>builder()
                .withInputMapper(message -> (DesignDocumentUpdateCompleted) message.getValue().getData())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentDeleteCompletedHandler(Controller<DesignDocumentDeleteCompleted, Void> controller) {
        return TemplateHandler.<InputMessage<Object>, DesignDocumentDeleteCompleted, Void, Void>builder()
                .withInputMapper(message -> (DesignDocumentDeleteCompleted) message.getValue().getData())
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }
}
