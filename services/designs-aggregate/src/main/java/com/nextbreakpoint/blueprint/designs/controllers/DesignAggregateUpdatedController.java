package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import org.apache.avro.specific.SpecificRecord;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdatedController implements Controller<InputMessage<DesignAggregateUpdated>, Void> {
    private final String messageSource;
    private final MessageEmitter<SpecificRecord> emitter;

    public DesignAggregateUpdatedController(String messageSource, MessageEmitter<SpecificRecord> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignAggregateUpdated> message) {
        return Single.just(message.getValue().getData())
                .map(this::onUpdateReceived)
                .flatMap(emitter::send);
    }

    private OutputMessage<SpecificRecord> onUpdateReceived(DesignAggregateUpdated event) {
        if (DesignAggregateStatus.DELETED.equals(event.getStatus())) {
            return Messages.asSpecificMessage(createMessage(createDeleteEvent(event)), x -> x);
        } else {
            return Messages.asSpecificMessage(createMessage(createUpdateEvent(event)), x -> x);
        }
    }

    private OutputMessage<DesignDocumentUpdateRequested> createMessage(DesignDocumentUpdateRequested event) {
        return MessageFactory.<DesignDocumentUpdateRequested>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private OutputMessage<DesignDocumentDeleteRequested> createMessage(DesignDocumentDeleteRequested event) {
        return MessageFactory.<DesignDocumentDeleteRequested>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private DesignDocumentUpdateRequested createUpdateEvent(DesignAggregateUpdated event) {
        return DesignDocumentUpdateRequested.newBuilder()
                .setDesignId(event.getDesignId())
                .setCommandId(event.getCommandId())
                .setUserId(event.getUserId())
                .setRevision(event.getRevision())
                .setChecksum(event.getChecksum())
                .setData(event.getData())
                .setStatus(DesignDocumentStatus.valueOf(event.getStatus().name()))
                .setPublished(event.getPublished())
                .setLevels(event.getLevels())
                .setTiles(Bitmap.of(event.getBitmap()).toTiles())
                .setCreated(event.getCreated())
                .setUpdated(event.getUpdated())
                .build();
    }

    private DesignDocumentDeleteRequested createDeleteEvent(DesignAggregateUpdated event) {
        return DesignDocumentDeleteRequested.newBuilder()
                .setDesignId(event.getDesignId())
                .setCommandId(event.getCommandId())
                .setRevision(event.getRevision())
                .build();
    }
}
