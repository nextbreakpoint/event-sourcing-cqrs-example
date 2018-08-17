package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.DefaultHandler;
import com.nextbreakpoint.shop.common.RequestFailedHandler;
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
import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import com.nextbreakpoint.shop.common.DeleteDesignsEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import com.nextbreakpoint.shop.common.InsertDesignEvent;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import com.nextbreakpoint.shop.common.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, InsertDesignEvent, InsertDesignResult, Content> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, InsertDesignEvent, InsertDesignResult, Content>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(new InsertDesignOutputMapper())
                .withController(new InsertDesignController(topic, producer, new InsertDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(201))
                .onFailure(new RequestFailedHandler())
                .build();
    }

    public static DefaultHandler<RoutingContext, UpdateDesignEvent, UpdateDesignResult, Content> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, UpdateDesignEvent, UpdateDesignResult, Content>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(new UpdateDesignOutputMapper())
                .withController(new UpdateDesignController(topic, producer, new UpdateDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new RequestFailedHandler())
                .build();
    }

    public static DefaultHandler<RoutingContext, DeleteDesignEvent, DeleteDesignResult, Content> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, DeleteDesignEvent, DeleteDesignResult, Content>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(new DeleteDesignOutputMapper())
                .withController(new DeleteDesignController(topic, producer, new DeleteDesignMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new RequestFailedHandler())
                .build();
    }

    public static DefaultHandler<RoutingContext, DeleteDesignsEvent, DeleteDesignsResult, Content> createDeleteDesignsHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, DeleteDesignsEvent, DeleteDesignsResult, Content>builder()
                .withInputMapper(new DeleteDesignsInputMapper())
                .withOutputMapper(new DeleteDesignsOutputMapper())
                .withController(new DeleteDesignsController(topic, producer, new DeleteDesignsMessageMapper(messageSource)))
                .onSuccess(new ContentHandler(200))
                .onFailure(new RequestFailedHandler())
                .build();
    }
}
