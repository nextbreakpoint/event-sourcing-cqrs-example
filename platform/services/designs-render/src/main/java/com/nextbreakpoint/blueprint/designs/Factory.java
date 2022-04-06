package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.MessageEmitter;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import com.nextbreakpoint.blueprint.designs.controllers.TileRenderRequestedController;
import com.nextbreakpoint.blueprint.designs.operations.download.*;
import com.nextbreakpoint.blueprint.designs.operations.upload.*;
import com.nextbreakpoint.blueprint.designs.operations.validate.*;
import io.vertx.core.Handler;
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

    public static RxSingleHandler<InputMessage, ?> createTileRenderRequestedHandler(String topic, KafkaProducer<String, String> producer, String messageSource, WorkerExecutor executor, S3AsyncClient s3AsyncClient, String bucket) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderRequestedController(
                        new TileRenderRequestedInputMapper(),
                        new TileRenderCompletedOutputMapper(messageSource, Render::createRenderKey),
                        new MessageEmitter(producer, topic, 3),
                        executor,
                        new S3Driver(s3AsyncClient, bucket),
                        new TileRenderer()
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
