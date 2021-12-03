package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.DesignAccumulator;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DesignAggregateManager {
    private final DesignAggregateStrategy strategy;

    private final Store store;

    public DesignAggregateManager(Store store, DesignAggregateStrategy strategy) {
        this.store = Objects.requireNonNull(store);
        this.strategy = Objects.requireNonNull(strategy);
    }

    public Single<Void> appendMessage(InputMessage message) {
        return store.appendMessage(message);
    }

    public Single<Optional<Design>> findDesign(UUID uuid) {
        return store.findDesign(uuid);
    }

    public Single<Optional<Design>> updateDesign(UUID uuid, long esid) {
        return store.findDesign(uuid)
                .flatMap(result -> store.findMessages(uuid, result.map(Design::getEsid).orElse(-1L), esid).flatMap(messages -> updateDesign(messages, result.map(this::convertToAccumulator).orElse(null))));
    }

    private Single<Optional<Design>> updateDesign(List<InputMessage> messages, DesignAccumulator accumulator) {
        return Single.fromCallable(() -> strategy.mergeEvents(accumulator, messages))
                .map(result -> result.map(DesignAccumulator::toDesign))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap(result -> result.map(this::updateDesign).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> updateDesign(Design design) {
        return store.updateDesign(design).map(ignore -> Optional.ofNullable(design));
    }

    private DesignAccumulator convertToAccumulator(Design design) {
        final Map<Integer, Tiles> tilesMap = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
        return new DesignAccumulator(design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), design.getChecksum(), design.getStatus(), design.getLevels(), tilesMap, design.getUpdated());
    }
}
