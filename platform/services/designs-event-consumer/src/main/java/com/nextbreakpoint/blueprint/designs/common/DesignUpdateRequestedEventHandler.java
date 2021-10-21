package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.designs.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import rx.Single;

import java.util.Objects;
import java.util.function.BiFunction;

public class DesignUpdateRequestedEventHandler implements BiFunction<EventMetadata, DesignUpdateRequested, Single<AggregateUpdateRequested>> {
    private Store store;

    public DesignUpdateRequestedEventHandler(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    public Single<AggregateUpdateRequested> apply(EventMetadata metadata, DesignUpdateRequested event) {
        return store.updateDesign(metadata.getTimeUUID(), event.getUuid(), event.getJson())
                .map(result -> new AggregateUpdateRequested(event.getUuid(), event.getTimestamp()));
    }
}
