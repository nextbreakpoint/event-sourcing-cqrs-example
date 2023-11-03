package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DesignEventStore {
    private static final String REVISION_NULL = "0000000000000000-0000000000000000";

    private final DesignMergeStrategy strategy;

    private final Store store;

    public DesignEventStore(Store store, DesignMergeStrategy strategy) {
        this.store = Objects.requireNonNull(store);
        this.strategy = Objects.requireNonNull(strategy);
    }

    public Single<Void> appendMessage(InputMessage message) {
        return store.appendMessage(message);
    }

    public Single<Optional<Design>> findDesign(UUID uuid) {
        return store.findDesign(uuid);
    }

    public Single<Optional<Design>> projectDesign(UUID uuid, String revision) {
        return store.findDesign(uuid)
                .flatMap(result -> projectDesign(uuid, revision, result.orElse(null)));
    }

    public Single<Optional<Design>> updateDesign(Design design) {
        return store.updateDesign(design).map(ignore -> Optional.of(design));
    }

    private Single<Optional<Design>> projectDesign(UUID uuid, String revision, Design design) {
        if (design == null) {
            return store.findMessages(uuid, REVISION_NULL, revision)
                    .flatMap(messages -> projectDesign(messages, null));
        } else {
            return store.findMessages(uuid, design.getRevision(), revision)
                    .flatMap(messages -> projectDesign(messages, design));
        }
    }

    private Single<Optional<Design>> projectDesign(List<InputMessage> messages, Design state) {
        return Single.fromCallable(() -> strategy.applyEvents(state, messages));
    }
}
