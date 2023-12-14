package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;

public class Factory {
    private Factory() {}

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentUpdateCompletedHandler(Controller<InputMessage<DesignDocumentUpdateCompleted>, Void> controller) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDocumentUpdateCompleted>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDocumentUpdateCompleted) data))
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentDeleteCompletedHandler(Controller<InputMessage<DesignDocumentDeleteCompleted>, Void> controller) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDocumentDeleteCompleted>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDocumentDeleteCompleted) data))
                .withOutputMapper(output -> output)
                .withController(controller)
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }
}
