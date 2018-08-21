package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.handlers.ContentConsumer;
import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsInputMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignOutputMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
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
                .onSuccess(new ContentConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, UpdateDesignEvent, UpdateDesignResult, Content> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, UpdateDesignEvent, UpdateDesignResult, Content>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(new UpdateDesignOutputMapper())
                .withController(new UpdateDesignController(topic, producer, new UpdateDesignMessageMapper(messageSource)))
                .onSuccess(new ContentConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, DeleteDesignEvent, DeleteDesignResult, Content> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, DeleteDesignEvent, DeleteDesignResult, Content>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(new DeleteDesignOutputMapper())
                .withController(new DeleteDesignController(topic, producer, new DeleteDesignMessageMapper(messageSource)))
                .onSuccess(new ContentConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, DeleteDesignsEvent, DeleteDesignsResult, Content> createDeleteDesignsHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, DeleteDesignsEvent, DeleteDesignsResult, Content>builder()
                .withInputMapper(new DeleteDesignsInputMapper())
                .withOutputMapper(new DeleteDesignsOutputMapper())
                .withController(new DeleteDesignsController(topic, producer, new DeleteDesignsMessageMapper(messageSource)))
                .onSuccess(new ContentConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
