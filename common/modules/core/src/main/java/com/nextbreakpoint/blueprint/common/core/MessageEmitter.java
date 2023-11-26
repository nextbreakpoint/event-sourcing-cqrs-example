package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

public interface MessageEmitter<T> {
    Single<Void> send(OutputMessage<T> message);

    Single<Void> send(OutputMessage<T> message, String topicName);

    String getTopicName();
}
