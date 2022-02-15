package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import rx.Single;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

public class TileAggregateUpdateCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateCompleted> inputMapper;
    private final MessageMapper<DesignDocumentUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregate aggregate;

    public TileAggregateUpdateCompletedController(DesignAggregate aggregate, Mapper<InputMessage, TileAggregateUpdateCompleted> inputMapper, MessageMapper<DesignDocumentUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateCompleted)
                .map(event -> outputMapper.transform(Tracing.from(message.getTrace()), event))
                .flatMap(emitter::onNext);
    }

    private Single<DesignDocumentUpdateRequested> onAggregateUpdateCompleted(TileAggregateUpdateCompleted event) {
        return aggregate.findDesign(event.getDesignId())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getDesignId())))
                .map(this::createEvent);
    }

    private DesignDocumentUpdateRequested createEvent(Design design) {
        return DesignDocumentUpdateRequested.builder()
                .withEventId(design.getEventId())
                .withDesignId(design.getDesignId())
                .withRevision(design.getRevision())
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withStatus(design.getStatus())
                .withLevels(design.getLevels())
                .withTiles(design.getTiles().values().stream().sorted(Comparator.comparing(Tiles::getLevel)).map(this::createTiles).collect(Collectors.toList()))
                .withModified(design.getModified())
                .build();
    }

    private DesignDocumentUpdateRequested.Tiles createTiles(Tiles tiles) {
        return DesignDocumentUpdateRequested.Tiles.builder()
                .withLevel(tiles.getLevel())
                .withRequested(tiles.getRequested())
                .withCompleted(tiles.getCompleted())
                .withFailed(tiles.getFailed())
                .build();
    }
}
