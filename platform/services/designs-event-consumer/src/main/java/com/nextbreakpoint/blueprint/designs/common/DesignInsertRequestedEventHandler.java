package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.designs.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import rx.Single;

import java.util.Objects;
import java.util.function.BiFunction;

public class DesignInsertRequestedEventHandler implements BiFunction<EventMetadata, DesignInsertRequested, Single<AggregateUpdateRequested>> {
    private Store store;

    public DesignInsertRequestedEventHandler(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    public Single<AggregateUpdateRequested> apply(EventMetadata metadata, DesignInsertRequested event) {
        return store.insertDesign(metadata.getTimeUUID(), event.getUuid(), event.getJson())
                .map(result -> new AggregateUpdateRequested(event.getUuid(), event.getTimestamp()));
    }
}