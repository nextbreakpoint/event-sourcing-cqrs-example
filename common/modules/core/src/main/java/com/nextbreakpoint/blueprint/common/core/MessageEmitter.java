package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

public interface MessageEmitter {
    Single<Void> send(OutputMessage message);

    Single<Void> send(OutputMessage message, String topicName);

    String getTopicName();
}
