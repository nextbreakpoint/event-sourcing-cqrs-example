package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.*;

public class DesignAggregate {
    private final DesignStateStrategy strategy;

    private final Store store;

    public DesignAggregate(Store store, DesignStateStrategy strategy) {
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
        return store.findDesign(uuid).flatMap(result -> updateDesign(uuid, esid, result.orElse(null)));
    }

    private Single<Optional<Design>> updateDesign(UUID uuid, long esid, Design design) {
        if (design == null) {
            return store.findMessages(uuid, -1, esid).flatMap(messages -> updateDesign(messages, null));
        } else {
            return store.findMessages(uuid, design.getEsid(), esid).flatMap(messages -> updateDesign(messages, design));
        }
    }

    private Single<Optional<Design>> updateDesign(List<InputMessage> messages, Design state) {
        return Single.fromCallable(() -> strategy.mergeEvents(state, messages))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap(result -> result.map(this::updateDesign).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> updateDesign(Design design) {
        if (design.getStatus().equals("DELETED")) {
            return store.deleteDesign(design).map(ignore -> Optional.ofNullable(design));
        } else {
            return store.updateDesign(design).map(ignore -> Optional.ofNullable(design));
        }
    }
}