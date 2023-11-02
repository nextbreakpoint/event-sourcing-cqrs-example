package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
public class BufferedTileRenderCompletedController implements Controller<List<InputMessage>, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final MessageMapper<TilesRendered, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignEventStore eventStore;

    public BufferedTileRenderCompletedController(
            DesignEventStore eventStore,
            Mapper<InputMessage, TileRenderCompleted> inputMapper,
            MessageMapper<TilesRendered, OutputMessage> outputMapper,
            MessageEmitter emitter
    ) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(List<InputMessage> messages) {
        return eventStore.findDesign(inputMapper.transform(messages.get(0)).getDesignId())
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMap(design -> sendEvents(design, messages))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<Void> sendEvents(Design design, List<InputMessage> messages) {
        return Observable.from(messages)
                .map(inputMapper::transform)
                .filter(event -> !isLateEvent(event, design))
                .map(this::createTile)
                .collect(ArrayList<Tile>::new, ArrayList::add)
                .map(tiles -> createEvent(design, tiles))
                .map(outputMapper::transform)
                .flatMapSingle(emitter::send);
    }

    private boolean isLateEvent(TileRenderCompleted event, Design design) {
        final boolean value = !event.getCommandId().equals(design.getCommandId());
        if (value) {
            log.debug("Discard late event {}", event);
        }
        return value;
    }

    private Tile createTile(TileRenderCompleted event) {
        return Tile.builder()
                .withLevel(event.getLevel())
                .withRow(event.getRow())
                .withCol(event.getCol())
                .build();
    }

    private TilesRendered createEvent(Design design, List<Tile> tiles) {
        return TilesRendered.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(design.getRevision())
                .withChecksum(design.getChecksum())
                .withData(design.getData())
                .withTiles(tiles)
                .build();
    }
}
