package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

public interface TombstoneEmitter {
    Single<Void> send(Tombstone tombstone);

    Single<Void> send(Tombstone tombstone, String topicName);

    String getTopicName();
}
