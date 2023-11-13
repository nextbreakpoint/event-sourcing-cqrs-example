package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateUpdatedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdated> inputMapper;
    private final Mapper<DesignDocumentUpdateRequested, OutputMessage> updateOutputMapper;
    private final Mapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper;
    private final MessageEmitter emitter;

    public DesignAggregateUpdatedController(
            Mapper<InputMessage, DesignAggregateUpdated> inputMapper,
            Mapper<DesignDocumentUpdateRequested, OutputMessage> updateOutputMapper,
            Mapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper,
            MessageEmitter emitter
    ) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.deleteOutputMapper = Objects.requireNonNull(deleteOutputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .map(this::onUpdateReceived)
                .flatMap(emitter::send);
    }

    private OutputMessage onUpdateReceived(DesignAggregateUpdated event) {
        if ("DELETED".equalsIgnoreCase(event.getStatus())) {
            return deleteOutputMapper.transform(createDeleteEvent(event));
        } else {
            return updateOutputMapper.transform(createUpdateEvent(event));
        }
    }

    private DesignDocumentUpdateRequested createUpdateEvent(DesignAggregateUpdated event) {
        final TilesBitmap bitmap = TilesBitmap.of(event.getBitmap());

        final List<Tiles> tiles = IntStream.range(0, 8)
                .mapToObj(bitmap::toTiles)
                .collect(Collectors.toList());

        return DesignDocumentUpdateRequested.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withUserId(event.getUserId())
                .withRevision(event.getRevision())
                .withChecksum(event.getChecksum())
                .withData(event.getData())
                .withStatus(event.getStatus())
                .withPublished(event.isPublished())
                .withLevels(event.getLevels())
                .withTiles(tiles)
                .withCreated(event.getCreated())
                .withUpdated(event.getUpdated())
                .build();
    }

    private DesignDocumentDeleteRequested createDeleteEvent(DesignAggregateUpdated event) {
        return DesignDocumentDeleteRequested.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withRevision(event.getRevision())
                .build();
    }
}
