package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Image;
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
import com.nextbreakpoint.blueprint.common.vertx.NotFoundConsumer;
import com.nextbreakpoint.blueprint.common.vertx.PNGConsumer;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.common.vertx.ZipConsumer;
import com.nextbreakpoint.blueprint.designs.common.AsyncTileRenderer;
import com.nextbreakpoint.blueprint.designs.common.BundleValidator;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import com.nextbreakpoint.blueprint.designs.controllers.TileRenderRequestedController;
import com.nextbreakpoint.blueprint.designs.controllers.get.GetImageController;
import com.nextbreakpoint.blueprint.designs.controllers.get.GetImageRequest;
import com.nextbreakpoint.blueprint.designs.controllers.get.GetImageRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.get.GetImageResponse;
import com.nextbreakpoint.blueprint.designs.controllers.get.GetImageResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.download.DownloadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderDesignController;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderDesignResponseMapper;
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

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createGetImageHandler(S3AsyncClient s3AsyncClient, String bucket) {
        return TemplateHandler.<RoutingContext, GetImageRequest, GetImageResponse, Optional<Image>>builder()
                .withInputMapper(new GetImageRequestMapper())
                .withController(new GetImageController(new S3Driver(s3AsyncClient, bucket)))
                .withOutputMapper(new GetImageResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new PNGConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createRenderDesignHandler(S3AsyncClient s3AsyncClient, String bucket) {
        return TemplateHandler.<RoutingContext, RenderDesignRequest, RenderDesignResponse, String>builder()
                .withInputMapper(new RenderDesignRequestMapper())
                .withController(new RenderDesignController(new S3Driver(s3AsyncClient, bucket)))
                .withOutputMapper(new RenderDesignResponseMapper())
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

    public static Handler<RoutingContext> createValidateDesignHandler() {
        return TemplateHandler.<RoutingContext, ValidateDesignRequest, ValidateDesignResponse, String>builder()
                .withInputMapper(new ValidateDesignRequestMapper())
                .withController(new ValidateDesignController(new BundleValidator()))
                .withOutputMapper(new ValidateDesignResponseMapper())
                .onSuccess(new JsonConsumer(200))
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
                        new S3Driver(s3AsyncClient, bucket),
                        new AsyncTileRenderer(executor, new TileRenderer())
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    private static <T> KafkaMessageEmitter<Payload, T> createEventMessageEmitter(KafkaProducer<String, Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createEventOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }
}
