package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.designs.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import rx.Single;

import java.util.Objects;
import java.util.function.BiFunction;

public class DesignDeleteRequestedEventHandler implements BiFunction<EventMetadata, DesignDeleteRequested, Single<AggregateUpdateRequested>> {
    private Store store;

    public DesignDeleteRequestedEventHandler(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    public Single<AggregateUpdateRequested> apply(EventMetadata metadata, DesignDeleteRequested event) {
        return store.deleteDesign(metadata.getTimeUUID(), event.getUuid())
                .map(result -> new AggregateUpdateRequested(event.getUuid(), event.getTimestamp()));
    }
}
