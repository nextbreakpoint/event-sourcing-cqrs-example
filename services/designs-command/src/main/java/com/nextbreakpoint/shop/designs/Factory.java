package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.FailedRequestHandler;
import com.nextbreakpoint.shop.common.RESTContentHandler;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignController;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignMessageMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignOutputMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsInputMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsMessageMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsOutputMapper;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignController;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignInputMapper;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignMessageMapper;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignOutputMapper;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignController;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignInputMapper;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignMessageMapper;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignOutputMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignEvent;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static RESTContentHandler<InsertDesignEvent, InsertDesignResponse> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return RESTContentHandler.<InsertDesignEvent, InsertDesignResponse>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(new InsertDesignOutputMapper())
                .withController(new InsertDesignController(topic, producer, new InsertDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(201))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    public static RESTContentHandler<UpdateDesignEvent, UpdateDesignResponse> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return RESTContentHandler.<UpdateDesignEvent, UpdateDesignResponse>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(new UpdateDesignOutputMapper())
                .withController(new UpdateDesignController(topic, producer, new UpdateDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    public static RESTContentHandler<DeleteDesignEvent, DeleteDesignResponse> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return RESTContentHandler.<DeleteDesignEvent, DeleteDesignResponse>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(new DeleteDesignOutputMapper())
                .withController(new DeleteDesignController(topic, producer, new DeleteDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    public static RESTContentHandler<DeleteDesignsEvent, DeleteDesignsResponse> createDeleteDesignsHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return RESTContentHandler.<DeleteDesignsEvent, DeleteDesignsResponse>builder()
                .withInputMapper(new DeleteDesignsInputMapper())
                .withOutputMapper(new DeleteDesignsOutputMapper())
                .withController(new DeleteDesignsController(topic, producer, new DeleteDesignsMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }
}
