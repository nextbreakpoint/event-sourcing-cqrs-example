package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
public class BufferedTileRenderCompletedController implements Controller<List<InputMessage<TileRenderCompleted>>, Void> {
    private final MessageEmitter<TilesRendered> emitter;
    private final DesignEventStore eventStore;
    private final String messageSource;

    public BufferedTileRenderCompletedController(String messageSource, DesignEventStore eventStore, MessageEmitter<TilesRendered> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(List<InputMessage<TileRenderCompleted>> messages) {
        return Single.fromCallable(() -> messages.get(0))
                .flatMap(event -> eventStore.findDesign(event.getValue().getData().getDesignId()))
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMap(design -> sendEvents(design, messages))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<Void> sendEvents(Design design, List<InputMessage<TileRenderCompleted>> messages) {
        return Observable.from(messages)
                .map(message -> message.getValue().getData())
                .map(event -> checkDesignId(event, design))
                .filter(event -> !isLateEvent(event, design))
                .map(this::createTile)
                .collect(ArrayList<Tile>::new, ArrayList::add)
                .map(tiles -> createEvent(design, tiles))
                .map(this::createMessage)
                .flatMapSingle(emitter::send);
    }

    private OutputMessage<TilesRendered> createMessage(TilesRendered event) {
        return MessageFactory.<TilesRendered>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private TileRenderCompleted checkDesignId(TileRenderCompleted event, Design design) {
        if (!event.getDesignId().equals(design.getDesignId())) {
            throw new IllegalArgumentException();
        }
        return event;
    }

    private boolean isLateEvent(TileRenderCompleted event, Design design) {
        final boolean value = !event.getCommandId().equals(design.getCommandId());
        if (value) {
            log.debug("Discard late event {}", event);
        }
        return value;
    }

    private Tile createTile(TileRenderCompleted event) {
        return Tile.newBuilder()
                .setLevel(event.getLevel())
                .setRow(event.getRow())
                .setCol(event.getCol())
                .build();
    }

    private TilesRendered createEvent(Design design, List<Tile> tiles) {
        return TilesRendered.newBuilder()
                .setDesignId(design.getDesignId())
                .setCommandId(design.getCommandId())
                .setRevision(design.getRevision())
                .setChecksum(design.getChecksum())
                .setData(design.getData())
                .setTiles(tiles)
                .build();
    }
}
