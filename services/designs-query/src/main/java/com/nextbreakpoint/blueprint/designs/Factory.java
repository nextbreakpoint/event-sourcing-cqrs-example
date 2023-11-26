package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.NotFoundConsumer;
import com.nextbreakpoint.blueprint.common.vertx.PNGConsumer;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDocumentDeleteRequestedController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDocumentUpdateRequestedController;
import com.nextbreakpoint.blueprint.designs.operations.get.GetTileController;
import com.nextbreakpoint.blueprint.designs.operations.get.GetTileRequest;
import com.nextbreakpoint.blueprint.designs.operations.get.GetTileRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.get.GetTileResponse;
import com.nextbreakpoint.blueprint.designs.operations.get.GetTileResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsController;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import io.vertx.core.Handler;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.kafka.clients.producer.KafkaProducer;
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

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDocumentUpdateRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDocumentUpdateRequested) data))
                .withOutputMapper(output -> output)
                .withController(new DesignDocumentUpdateRequestedController(
                        messageSource,
                        store,
                        createEventMessageEmitter(producer, topic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDocumentDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDocumentDeleteRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDocumentDeleteRequested) data))
                .withOutputMapper(output -> output)
                .withController(new DesignDocumentDeleteRequestedController(
                        messageSource,
                        store,
                        createEventMessageEmitter(producer, topic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    private static <T> KafkaMessageEmitter<Payload, T> createEventMessageEmitter(KafkaProducer<String, Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createEventOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }
}
