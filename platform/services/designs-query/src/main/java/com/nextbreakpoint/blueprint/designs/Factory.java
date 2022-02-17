package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.BlockingHandler;
import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDocumentUpdateRequestedController;
import com.nextbreakpoint.blueprint.designs.operations.get.*;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsController;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.persistence.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.persistence.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.LoadDesignResponse;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withController(new ListDesignsController(store))
                .withOutputMapper(new ListDesignsResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withController(new LoadDesignController(store))
                .withOutputMapper(new LoadDesignResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createGetTileHandler(Store store, S3AsyncClient s3AsyncClient, String s3Bucket) {
        return TemplateHandler.<RoutingContext, GetTileRequest, GetTileResponse, Optional<Image>>builder()
                .withInputMapper(new GetTileRequestMapper())
                .withController(new GetTileController(store, new S3Driver(s3AsyncClient, s3Bucket)))
                .withOutputMapper(new GetTileResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new PNGConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignDocumentUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignDocumentUpdateRequestedController(
                        store,
                        new DesignDocumentUpdateRequestedInputMapper(),
                        new DesignDocumentUpdateCompletedOutputMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}