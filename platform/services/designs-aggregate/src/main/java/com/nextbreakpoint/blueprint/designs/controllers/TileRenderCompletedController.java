package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileRenderCompletedController implements Controller<List<InputMessage>, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final MessageMapper<TilesRendered, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public TileRenderCompletedController(DesignAggregate aggregate, Mapper<InputMessage, TileRenderCompleted> inputMapper, MessageMapper<TilesRendered, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(List<InputMessage> messages) {
        return aggregate.findDesign(inputMapper.transform(messages.get(0)).getDesignId())
                .flatMapObservable(result -> result.map(design -> createEvents(design, messages)).orElseGet(Observable::empty))
                .map(outputMapper::transform)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TilesRendered> createEvents(Design design, List<InputMessage> messages) {
        return Observable.from(messages)
                .map(inputMapper::transform)
                .filter(event -> event.getCommandId().equals(design.getCommandId()))
                .filter(event -> event.getChecksum().equals(design.getChecksum()))
                .map(this::createTile)
                .collect(ArrayList<Tile>::new, ArrayList::add)
                .map(tiles -> createEvent(design, tiles));
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
