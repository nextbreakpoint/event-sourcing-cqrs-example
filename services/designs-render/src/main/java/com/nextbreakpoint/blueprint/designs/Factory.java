package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.common.vertx.ZipConsumer;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import com.nextbreakpoint.blueprint.designs.controllers.TileRenderRequestedController;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.upload.UploadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.upload.UploadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.upload.UploadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.upload.UploadDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.upload.UploadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.validate.ValidateDesignController;
import com.nextbreakpoint.blueprint.designs.operations.validate.ValidateDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.validate.ValidateDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.validate.ValidateDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.validate.ValidateDesignResponseMapper;
import io.vertx.core.Handler;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.kafka.clients.producer.KafkaProducer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createValidateDesignHandler() {
        return TemplateHandler.<RoutingContext, ValidateDesignRequest, ValidateDesignResponse, String>builder()
                .withInputMapper(new ValidateDesignRequestMapper())
                .withController(new ValidateDesignController())
                .withOutputMapper(new ValidateDesignResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUploadDesignHandler() {
        return TemplateHandler.<RoutingContext, UploadDesignRequest, UploadDesignResponse, String>builder()
                .withInputMapper(new UploadDesignRequestMapper())
                .withController(new UploadDesignController())
                .withOutputMapper(new UploadDesignResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createDownloadDesignHandler() {
        return TemplateHandler.<RoutingContext, DownloadDesignRequest, DownloadDesignResponse, byte[]>builder()
                .withInputMapper(new DownloadDesignRequestMapper())
                .withController(new DownloadDesignController())
                .withOutputMapper(new DownloadDesignResponseMapper())
                .onSuccess(new ZipConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createTileRenderRequestedHandler(String topic, KafkaProducer<String, Payload> producer, String messageSource, WorkerExecutor executor, S3AsyncClient s3AsyncClient, String bucket) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<TileRenderRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (TileRenderRequested) data))
                .withOutputMapper(output -> output)
                .withController(new TileRenderRequestedController(
                        messageSource,
                        createEventMessageEmitter(producer, topic),
                        executor,
                        new S3Driver(s3AsyncClient, bucket),
                        new TileRenderer()
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    private static <T> KafkaMessageEmitter<Payload, T> createEventMessageEmitter(KafkaProducer<String, Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createEventOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }
}
